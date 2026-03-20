# -*- coding: utf-8 -*-
"""
绿色生产核心模块
- 能效监测（PUE）
- 碳足迹算法
- AI 节能降碳报告
"""
import os
from datetime import datetime, timedelta
from typing import Optional

import httpx

from app.models.schemas import (
    DevicePowerData,
    EnergyMonitoringRequest,
    PUEResult,
    CarbonFootprintRequest,
    CarbonFootprintResult,
    EnergyOptimizationRequest,
)

# 电网碳因子 kg CO2/kWh（中国电网约 0.5~0.6）
GRID_CARBON_FACTOR = 0.55
# 材料排放因子 kg CO2/kg
MATERIAL_EMISSION_FACTORS = {
    "steel": 1.85,
    "aluminum": 8.0,
    "plastic": 3.5,
    "copper": 3.0,
    "default": 2.0,
}


def calculate_pue(req: EnergyMonitoringRequest) -> PUEResult:
    """
    能效监测：根据设备实时功率计算 PUE（能源效率）指标。
    PUE = 总设施能耗 / 生产设备能耗，越接近 1 越优
    """
    process_kw = sum(d.power_kw for d in req.devices if d.device_type == "process")
    auxiliary_kw = sum(d.power_kw for d in req.devices if d.device_type == "auxiliary")
    total_kw = process_kw + auxiliary_kw

    if req.facility_total_kw is not None and req.facility_total_kw > 0:
        total_kw = req.facility_total_kw

    if process_kw <= 0:
        process_kw = total_kw * 0.7  # 无生产设备时按 70% 估算
        auxiliary_kw = total_kw - process_kw

    pue = total_kw / process_kw if process_kw > 0 else 1.0
    pue = min(3.0, max(1.0, pue))  # 合理范围 1.0~3.0

    return PUEResult(
        pue=round(pue, 2),
        total_power_kw=round(total_kw, 2),
        process_power_kw=round(process_kw, 2),
        auxiliary_power_kw=round(auxiliary_kw, 2),
        device_count=len(req.devices),
    )


def calculate_carbon_footprint(req: CarbonFootprintRequest) -> CarbonFootprintResult:
    """
    碳足迹算法：根据生产工时和材料重量换算 CO2 排放量。
    - 能耗 CO2 = 工时(h) × 功率(kW) × 电网碳因子(kg/kWh)
    - 材料 CO2 = 重量(kg) × 材料排放因子(kg CO2/kg)
    """
    energy_kwh = req.production_hours * req.avg_power_kw
    energy_co2 = energy_kwh * GRID_CARBON_FACTOR

    factor = MATERIAL_EMISSION_FACTORS.get(req.material_type.lower(), MATERIAL_EMISSION_FACTORS["default"])
    material_co2 = req.material_weight_kg * factor

    total = energy_co2 + material_co2

    return CarbonFootprintResult(
        total_co2_kg=round(total, 2),
        energy_co2_kg=round(energy_co2, 2),
        material_co2_kg=round(material_co2, 2),
        breakdown={
            "energy_kwh": round(energy_kwh, 2),
            "grid_carbon_factor": GRID_CARBON_FACTOR,
            "material_factor": factor,
        },
    )


async def generate_energy_optimization_report(
    req: EnergyOptimizationRequest,
    *,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
) -> str:
    """
    根据当前能耗分布，调用大模型生成「节能降碳改进报告」。
    """
    api_key = api_key or os.getenv("LLM_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
    base_url = (base_url or os.getenv("LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")).rstrip("/")
    model = model or os.getenv("LLM_MODEL", "qwen-plus")

    prompt = (
        f"请根据以下能耗数据，生成一段「节能降碳改进报告」（200～300字）：\n\n"
        f"能耗分布（kW）：{req.energy_distribution}\n"
        f"PUE 指标：{req.pue}\n"
        f"总 CO2 排放：{req.total_co2_kg or '未提供'} kg\n\n"
        "要求：1. 指出主要能耗瓶颈 2. 给出 3～5 条可执行改进建议 3. 预估节能潜力 4. 语气专业简洁"
    )
    system = "你是工业能效与节能降碳专家，擅长分析能耗分布并给出改进建议。"

    if not api_key:
        return (
            "【节能降碳改进建议】\n"
            "1. 优化辅助设备（制冷、照明）运行策略，降低 PUE 至 1.5 以下\n"
            "2. 生产设备错峰运行，减少峰时用电\n"
            "3. 引入节能设备与变频控制\n"
            "4. 建议配置 LLM API Key 以获取 AI 生成的定制报告"
        )

    try:
        url = f"{base_url}/chat/completions"
        payload = {
            "model": model,
            "messages": [{"role": "system", "content": system}, {"role": "user", "content": prompt}],
            "temperature": 0.6,
            "max_tokens": 1024,
        }
        async with httpx.AsyncClient(timeout=60.0) as client:
            resp = await client.post(
                url,
                json=payload,
                headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
            )
            resp.raise_for_status()
            data = resp.json()
        choices = data.get("choices", [])
        if choices:
            return choices[0].get("message", {}).get("content", "").strip()
    except Exception:
        pass
    return "请检查 LLM 配置，暂时无法生成报告。"
