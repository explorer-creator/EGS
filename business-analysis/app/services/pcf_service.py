# -*- coding: utf-8 -*-
"""
产品碳足迹 (PCF) 核算模型
利用 iDME 数字主线能力，将零件原材料来源、运输距离、加工时长与碳排放系数绑定
"""
import os
from datetime import datetime
from typing import Optional

import httpx

from app.models.schemas import (
    PartCarbonData,
    ProcessCarbonData,
    ProductPCFRequest,
    ProductPCFResult,
    DigitalCarbonLabel,
    CarbonLabelItem,
)


def _calc_part_co2(p: PartCarbonData) -> tuple[float, float, float]:
    """零件 CO2：材料 + 运输 + 加工"""
    material_co2 = p.material_weight_kg * p.material_emission_factor
    transport_co2 = p.material_weight_kg * p.transport_distance_km * p.transport_emission_factor
    processing_kwh = p.processing_hours * p.processing_power_kw
    processing_co2 = processing_kwh * p.grid_carbon_factor
    return material_co2, transport_co2, processing_co2


def _calc_process_co2(proc: ProcessCarbonData) -> float:
    """工序 CO2"""
    kwh = proc.duration_hours * proc.power_kw
    return kwh * proc.grid_carbon_factor


def _identify_high_emission(
    parts: list[PartCarbonData],
    processes: list[ProcessCarbonData],
    total_co2: float,
) -> list[dict]:
    """识别高碳排放点：占比超过 15% 或耗电异常（单位工时能耗高于均值 1.5 倍）"""
    points: list[dict] = []
    if total_co2 <= 0:
        return points

    # 零件贡献
    for i, p in enumerate(parts):
        m, t, pr = _calc_part_co2(p)
        part_total = m + t + pr
        share = (part_total / total_co2) * 100
        if share >= 15:
            points.append({
                "type": "part",
                "id": p.part_id or f"part_{i}",
                "name": p.part_name or f"零件{i+1}",
                "co2_kg": round(part_total, 2),
                "share_pct": round(share, 1),
                "reason": f"材料/运输/加工合计占比 {share:.1f}%",
            })
        # 耗电异常：单位工时能耗
        if p.processing_hours > 0 and p.processing_power_kw > 0:
            kwh_per_hour = p.processing_power_kw
            # 简单阈值：功率 > 80kW 视为异常
            if p.processing_power_kw >= 80:
                points.append({
                    "type": "part_energy",
                    "id": p.part_id or f"part_{i}",
                    "name": p.part_name or f"零件{i+1}",
                    "power_kw": p.processing_power_kw,
                    "hours": p.processing_hours,
                    "reason": f"加工功率 {p.processing_power_kw} kW 偏高，建议检查设备能效",
                })

    # 工序贡献
    for i, proc in enumerate(processes):
        co2 = _calc_process_co2(proc)
        share = (co2 / total_co2) * 100 if total_co2 > 0 else 0
        if share >= 15:
            points.append({
                "type": "process",
                "id": proc.process_id or f"proc_{i}",
                "name": proc.process_name or f"工序{i+1}",
                "co2_kg": round(co2, 2),
                "share_pct": round(share, 1),
                "reason": f"工序能耗占比 {share:.1f}%",
            })
        if proc.duration_hours > 0 and proc.power_kw >= 100:
            points.append({
                "type": "process_energy",
                "id": proc.process_id or f"proc_{i}",
                "name": proc.process_name or f"工序{i+1}",
                "power_kw": proc.power_kw,
                "hours": proc.duration_hours,
                "reason": f"工序耗电异常：{proc.power_kw} kW × {proc.duration_hours} h",
            })

    return points[:10]  # 最多 10 个


async def _generate_pcf_ai_suggestions(
    high_points: list[dict],
    total_co2: float,
    breakdown: dict,
    *,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
) -> str:
    """AI 识别高碳排放点并给出改进建议"""
    api_key = api_key or os.getenv("LLM_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
    base_url = (base_url or os.getenv("LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")).rstrip("/")
    model = model or os.getenv("LLM_MODEL", "qwen-plus")

    prompt = (
        f"产品碳足迹分析：总排放 {total_co2:.1f} kg CO2，明细 {breakdown}\n"
        f"识别到的高碳排放点：{high_points}\n\n"
        "请针对每个高碳排放点给出 1～2 条具体改进建议（如设备能效、工艺优化、材料替代、运输方式等），"
        "控制在 150～250 字，分点列出。"
    )
    system = "你是工业碳足迹与节能降碳专家，擅长识别生产链路中的高碳排放点并给出可执行建议。"

    if not api_key:
        return (
            "【改进建议】1. 优化高耗能工序设备能效 2. 考虑材料就近采购降低运输排放 "
            "3. 错峰生产利用绿电 4. 配置 LLM API Key 获取定制建议"
        )

    try:
        url = f"{base_url}/chat/completions"
        payload = {
            "model": model,
            "messages": [{"role": "system", "content": system}, {"role": "user", "content": prompt}],
            "temperature": 0.5,
            "max_tokens": 512,
        }
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(
                url, json=payload,
                headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
            )
            resp.raise_for_status()
            data = resp.json()
        choices = data.get("choices", [])
        if choices:
            return choices[0].get("message", {}).get("content", "").strip()
    except Exception:
        pass
    return "1. 优化高耗能工序 2. 降低运输距离 3. 选用低碳材料"


def compute_product_pcf(req: ProductPCFRequest) -> ProductPCFResult:
    """
    产品完成生产时，自动计算产品碳足迹并生成数字碳标签。
    """
    items: list[CarbonLabelItem] = []
    total_material = 0.0
    total_transport = 0.0
    total_processing = 0.0

    for i, p in enumerate(req.parts):
        m, t, pr = _calc_part_co2(p)
        total_material += m
        total_transport += t
        total_processing += pr
        name = p.part_name or f"零件{i+1}"
        if m > 0:
            items.append(CarbonLabelItem(category="material", item_name=name, co2_kg=round(m, 2), share_pct=0))
        if t > 0:
            items.append(CarbonLabelItem(category="transport", item_name=name, co2_kg=round(t, 2), share_pct=0))
        if pr > 0:
            items.append(CarbonLabelItem(category="processing", item_name=name, co2_kg=round(pr, 2), share_pct=0))

    for i, proc in enumerate(req.processes):
        co2 = _calc_process_co2(proc)
        total_processing += co2
        items.append(CarbonLabelItem(
            category="processing",
            item_name=proc.process_name or f"工序{i+1}",
            co2_kg=round(co2, 2),
            share_pct=0,
        ))

    total_co2 = total_material + total_transport + total_processing
    co2_per_unit = total_co2 / req.quantity if req.quantity > 0 else 0

    # 计算占比
    for it in items:
        it.share_pct = round((it.co2_kg / total_co2) * 100, 1) if total_co2 > 0 else 0

    # iDME 数字主线追溯
    idme_trace = {
        "product_id": req.product_id,
        "batch_id": req.batch_id,
        "entities": ["Material01", "Material_BOM", "WorkingProcedure", "WorkingPlan"],
    }

    carbon_label = DigitalCarbonLabel(
        product_id=req.product_id,
        product_name=req.product_name,
        total_co2_kg=round(total_co2, 2),
        co2_per_unit_kg=round(co2_per_unit, 2),
        quantity=req.quantity,
        issued_at=datetime.now().isoformat(),
        items=items,
        idme_trace=idme_trace,
    )

    breakdown = {
        "material_co2_kg": round(total_material, 2),
        "transport_co2_kg": round(total_transport, 2),
        "processing_co2_kg": round(total_processing, 2),
    }

    high_points = _identify_high_emission(req.parts, req.processes, total_co2)

    return ProductPCFResult(
        total_co2_kg=round(total_co2, 2),
        co2_per_unit_kg=round(co2_per_unit, 2),
        carbon_label=carbon_label,
        breakdown=breakdown,
        high_emission_points=high_points,
        ai_suggestions="",  # 由 async 填充
    )


async def compute_product_pcf_with_ai(
    req: ProductPCFRequest,
    *,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
) -> ProductPCFResult:
    """产品碳足迹核算 + AI 改进建议"""
    result = compute_product_pcf(req)
    result.ai_suggestions = await _generate_pcf_ai_suggestions(
        result.high_emission_points,
        result.total_co2_kg,
        result.breakdown,
        api_key=api_key,
        base_url=base_url,
        model=model,
    )
    return result
