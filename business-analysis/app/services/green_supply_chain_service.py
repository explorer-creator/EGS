# -*- coding: utf-8 -*-
"""
绿色供应链评分服务
- AI 分析供应商环保报告、ISO 14001 认证
- 采购建议：综合考虑价格与碳强度，便宜但排碳高时黄色预警，推荐更环保替代
"""
import os
from typing import Optional

import httpx

from app.models.schemas import (
    SupplierInput,
    GreenSupplyChainScoreRequest,
    GreenSupplyChainScoreResult,
    SupplierGreenScore,
    ProcurementRecommendationRequest,
    ProcurementRecommendationResult,
)


async def _call_llm(
    system_content: str,
    user_content: str,
    api_key: str,
    base_url: str,
    model: str,
) -> str:
    """通用 LLM 调用"""
    url = f"{base_url.rstrip('/')}/chat/completions"
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": system_content},
            {"role": "user", "content": user_content},
        ],
        "temperature": 0.5,
        "max_tokens": 2048,
    }
    async with httpx.AsyncClient(timeout=90.0) as client:
        resp = await client.post(
            url,
            json=payload,
            headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
        )
        resp.raise_for_status()
        data = resp.json()
    choices = data.get("choices", [])
    if not choices:
        return ""
    msg = choices[0].get("message", {})
    return msg.get("content", "").strip()


def _infer_green_score_from_data(s: SupplierInput) -> SupplierGreenScore:
    """
    无 LLM 时的规则推断：根据碳强度、ISO 14001 计算绿色评分。
    碳强度越低、有认证则分数越高。
    """
    base = 50.0
    if s.iso14001_certified:
        base += 25
    elif s.iso14001_certified is False:
        base -= 10
    # 碳强度：假设 0.5 kg/件为中等，每高 0.1 减 2 分，每低 0.1 加 2 分
    if s.carbon_intensity > 0:
        delta = (0.5 - s.carbon_intensity) * 20
        base += max(-30, min(30, delta))
    score = max(0, min(100, round(base, 1)))
    iso_status = "已认证" if s.iso14001_certified else ("未认证" if s.iso14001_certified is False else "待核实")
    report_summary = s.environmental_report_text[:200] + "..." if s.environmental_report_text and len(s.environmental_report_text) > 200 else (s.environmental_report_text or "未提供报告")
    strengths = ["ISO 14001 认证"] if s.iso14001_certified else []
    if s.carbon_intensity > 0 and s.carbon_intensity < 0.3:
        strengths.append("碳强度较低")
    risks = []
    if s.carbon_intensity > 1.0:
        risks.append("碳强度偏高")
    if s.iso14001_certified is False:
        risks.append("未通过 ISO 14001")
    return SupplierGreenScore(
        supplier_id=s.supplier_id,
        supplier_name=s.supplier_name,
        green_score=score,
        unit_price=s.unit_price,
        carbon_intensity=s.carbon_intensity,
        iso14001_status=iso_status,
        report_summary=report_summary,
        strengths=strengths,
        risks=risks,
    )


async def score_suppliers_green(
    req: GreenSupplyChainScoreRequest,
    *,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
) -> GreenSupplyChainScoreResult:
    """
    绿色供应链评分：AI 分析供应商环保报告、ISO 14001 认证情况。
    若提供 environmental_report_text，LLM 分析；否则按规则推断。
    """
    api_key = api_key or os.getenv("LLM_API_KEY") or os.getenv("OPENAI_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
    base_url = (base_url or os.getenv("LLM_BASE_URL") or "https://dashscope.aliyuncs.com/compatible-mode/v1").rstrip("/")
    model = model or os.getenv("LLM_MODEL", "qwen-plus")

    has_report = any(s.environmental_report_text for s in req.suppliers)
    use_llm = api_key and has_report
    content = ""

    if use_llm:
        # 构建 LLM 提示：分析环保报告与 ISO 14001
        lines = [
            "你是一位绿色供应链专家。请根据以下供应商信息，分析其环保报告、ISO 14001 认证情况，给出绿色评分（0-100）。",
            "",
            f"采购产品：{req.product_name or '通用物料'}",
            f"优先级：{req.priority}",
            "",
            "【供应商列表】",
        ]
        for i, s in enumerate(req.suppliers, 1):
            lines.append(f"\n供应商 {i}：{s.supplier_name}")
            lines.append(f"  单价：{s.unit_price} 元/件，碳强度：{s.carbon_intensity} kg CO2/件")
            lines.append(f"  ISO 14001：{'是' if s.iso14001_certified else '否' if s.iso14001_certified is False else '未知'}")
            if s.environmental_report_text:
                lines.append(f"  环保报告摘要：")

                lines.append(f"  {s.environmental_report_text[:1500]}")

        lines.extend([
            "",
            "请按 JSON 格式输出，每个供应商一行，格式：",
            '{"supplier_name": "名称", "green_score": 85, "iso14001_status": "已认证/未认证/待核实", "report_summary": "分析摘要", "strengths": ["优势1"], "risks": ["风险1"]}',
            "若无法解析为 JSON，则用自然语言描述各供应商评分与建议。",
        ])

        try:
            system = "你是绿色供应链分析师，擅长从环保报告中提取 ISO 14001、碳减排、循环经济等关键信息。输出简洁、可执行的建议。"
            content = await _call_llm(system, "\n".join(lines), api_key, base_url, model)
        except Exception:
            use_llm = False

    if use_llm and content:
        # 解析 LLM 返回（可能为 JSON 或自然语言）
        import json
        import re
        scores_map = {}
        for block in re.findall(r"\{[^{}]*\"supplier_name\"[^{}]*\}", content):
            try:
                obj = json.loads(block)
                name = obj.get("supplier_name", "")
                if name:
                    scores_map[name] = obj
            except json.JSONDecodeError:
                pass

        suppliers = []
        for s in req.suppliers:
            if s.supplier_name in scores_map:
                o = scores_map[s.supplier_name]
                suppliers.append(SupplierGreenScore(
                    supplier_id=s.supplier_id,
                    supplier_name=s.supplier_name,
                    green_score=float(o.get("green_score", 50)),
                    unit_price=s.unit_price,
                    carbon_intensity=s.carbon_intensity,
                    iso14001_status=str(o.get("iso14001_status", "待核实")),
                    report_summary=str(o.get("report_summary", "")),
                    strengths=o.get("strengths", []) if isinstance(o.get("strengths"), list) else [],
                    risks=o.get("risks", []) if isinstance(o.get("risks"), list) else [],
                ))
            else:
                suppliers.append(_infer_green_score_from_data(s))
    else:
        suppliers = [_infer_green_score_from_data(s) for s in req.suppliers]

    # 黄色预警：便宜但排碳高
    avg_carbon = sum(s.carbon_intensity for s in suppliers) / len(suppliers) if suppliers else 0
    warnings = []
    for s in suppliers:
        if s.carbon_intensity > avg_carbon * 1.5 and s.unit_price < sum(x.unit_price for x in suppliers) / max(len(suppliers), 1) * 1.1:
            warnings.append(f"供应商「{s.supplier_name}」价格较低但碳强度偏高（{s.carbon_intensity} kg/件），建议评估环保替代方案。")

    ai_analysis = content if use_llm and content and not content.strip().startswith("{") else (
        f"已对 {len(suppliers)} 家供应商进行绿色评分。"
        + ("存在价格低但排碳高的供应商，建议优先考虑绿色认证供应商。" if warnings else "各供应商碳强度在合理范围内。")
    )

    return GreenSupplyChainScoreResult(
        success=True,
        suppliers=suppliers,
        ai_analysis=ai_analysis,
        warnings=warnings,
    )


def get_procurement_recommendation(req: ProcurementRecommendationRequest) -> ProcurementRecommendationResult:
    """
    采购建议：综合考虑价格与碳强度。
    若某供应商便宜但排碳极高，给出黄色预警并推荐更环保替代。
    """
    if not req.suppliers:
        return ProcurementRecommendationResult(
            success=False,
            recommended_supplier="",
            recommended_reason="无候选供应商",
            ai_recommendation="请提供至少一家候选供应商。",
        )

    # 构建综合得分：价格归一化 + 碳强度归一化，按权重加权
    prices = [s.unit_price for s in req.suppliers]
    carbons = [s.carbon_intensity for s in req.suppliers]
    min_p, max_p = min(prices), max(prices)
    min_c, max_c = min(carbons), max(carbons)
    price_range = max_p - min_p if max_p > min_p else 1
    carbon_range = max_c - min_c if max_c > min_c else 1

    scored = []
    for s in req.suppliers:
        price_score = 1 - (s.unit_price - min_p) / price_range  # 便宜得分高
        carbon_score = 1 - (s.carbon_intensity - min_c) / carbon_range  # 低碳得分高
        combined = (1 - req.green_weight) * price_score + req.green_weight * carbon_score
        scored.append((s, combined, price_score, carbon_score))

    scored.sort(key=lambda x: x[1], reverse=True)
    best = scored[0][0]

    # 黄色预警：价格低但碳高
    yellow_warnings = []
    avg_carbon = sum(carbons) / len(carbons)
    for s in req.suppliers:
        if s.carbon_intensity > avg_carbon * 1.5:
            price_rank = sum(1 for p in prices if p < s.unit_price) / len(prices) * 100
            if price_rank < 50:  # 价格在前半（较便宜）
                yellow_warnings.append({
                    "supplier": s.supplier_name,
                    "price": s.unit_price,
                    "carbon_intensity": s.carbon_intensity,
                    "message": f"该供应商价格较低但碳强度偏高（{s.carbon_intensity} kg/件），存在环保风险。",
                })

    # 更环保的替代：碳强度低于当前推荐且价格可接受
    greener = []
    best_carbon = best.carbon_intensity
    for s in req.suppliers:
        if s.supplier_name != best.supplier_name and s.carbon_intensity < best_carbon:
            green_score = max(0, 100 - s.carbon_intensity * 50)  # 粗略绿色分
            greener.append({
                "supplier": s.supplier_name,
                "price": s.unit_price,
                "carbon_intensity": s.carbon_intensity,
                "green_score": round(green_score, 1),
            })
    greener.sort(key=lambda x: x["carbon_intensity"])

    reason = f"综合价格与碳强度（绿色权重 {req.green_weight}），推荐「{best.supplier_name}」。"
    if best.carbon_intensity < avg_carbon:
        reason += "该供应商碳强度低于平均水平。"
    else:
        reason += "若优先考虑环保，可参考下方更环保替代方案。"

    ai_rec = (
        f"推荐采购「{best.supplier_name}」。"
        + (f"注意：存在 {len(yellow_warnings)} 家价格低但排碳高的供应商，建议谨慎选择。" if yellow_warnings else "")
        + (f"更环保替代：{', '.join(g['supplier'] for g in greener[:3])}。" if greener else "")
    )

    return ProcurementRecommendationResult(
        success=True,
        recommended_supplier=best.supplier_name,
        recommended_reason=reason,
        yellow_warnings=yellow_warnings,
        greener_alternatives=greener[:5],
        ai_recommendation=ai_rec,
    )


async def get_procurement_recommendation_with_ai(
    req: ProcurementRecommendationRequest,
    *,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
) -> ProcurementRecommendationResult:
    """
    采购建议（含 LLM 增强）：先做规则推荐，再调用 LLM 生成更丰富的 AI 建议。
    """
    base = get_procurement_recommendation(req)
    api_key = api_key or os.getenv("LLM_API_KEY") or os.getenv("OPENAI_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
    if not api_key:
        return base

    base_url = (base_url or os.getenv("LLM_BASE_URL") or "https://dashscope.aliyuncs.com/compatible-mode/v1").rstrip("/")
    model = model or os.getenv("LLM_MODEL", "qwen-plus")

    lines = [
        f"采购产品：{req.product_name or '通用物料'}，数量：{req.quantity}",
        f"绿色权重：{req.green_weight}（0=只看价格，1=只看碳）",
        "",
        "【候选供应商】",
    ]
    for s in req.suppliers:
        lines.append(f"- {s.supplier_name}：单价 {s.unit_price} 元，碳强度 {s.carbon_intensity} kg CO2/件")
    lines.extend([
        "",
        f"【系统推荐】{base.recommended_supplier}",
        f"【黄色预警】{[w['supplier'] for w in base.yellow_warnings]}",
        f"【更环保替代】{[g['supplier'] for g in base.greener_alternatives]}",
        "",
        "请用 2-3 句话给出采购建议，综合考虑价格与碳强度，对便宜但排碳高的供应商给出风险提示。",
    ])

    try:
        system = "你是采购分析师，擅长在成本与环保之间平衡，给出务实建议。"
        ai_rec = await _call_llm(system, "\n".join(lines), api_key, base_url, model)
        if ai_rec:
            base.ai_recommendation = ai_rec
    except Exception:
        pass

    return base
