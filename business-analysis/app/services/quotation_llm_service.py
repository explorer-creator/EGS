# -*- coding: utf-8 -*-
"""
报价模块 LLM 增强服务
- 报价单 PDF 自动生成
- 谈判策略助手
- 多方案对比
"""
import io
import os
import re
from datetime import datetime
from typing import Optional

import httpx
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle

from app.models.schemas import (
    QuotationDataForPdf,
    NegotiationStrategyRequest,
    NegotiationStrategyResult,
    MultiPlanComparisonRequest,
    MultiPlanComparisonResult,
    QuotationPlan,
)
from app.services.success_rate_service import predict_success_rate


async def _call_llm(system: str, user: str, api_key: str, base_url: str, model: str) -> str:
    url = f"{base_url.rstrip('/')}/chat/completions"
    payload = {
        "model": model,
        "messages": [{"role": "system", "content": system}, {"role": "user", "content": user}],
        "temperature": 0.6,
        "max_tokens": 2048,
    }
    async with httpx.AsyncClient(timeout=60.0) as client:
        resp = await client.post(
            url, json=payload,
            headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
        )
        resp.raise_for_status()
        data = resp.json()
    choices = data.get("choices", [])
    if not choices:
        return ""
    return choices[0].get("message", {}).get("content", "").strip()


def _build_pdf_from_data(data: QuotationDataForPdf, llm_summary: str) -> bytes:
    """根据报价数据和 LLM 摘要生成 PDF"""
    buffer = io.BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=A4, rightMargin=2*cm, leftMargin=2*cm, topMargin=2*cm, bottomMargin=2*cm)
    styles = getSampleStyleSheet()
    title_style = ParagraphStyle(name="Title", parent=styles["Heading1"], fontSize=18, spaceAfter=12)
    h2_style = ParagraphStyle(name="H2", parent=styles["Heading2"], fontSize=14, spaceAfter=8)

    story = []
    story.append(Paragraph("报价建议书", title_style))
    story.append(Paragraph(f"产品：{data.product_name}", styles["Normal"]))
    story.append(Paragraph(f"报价日期：{datetime.now().strftime('%Y-%m-%d')}", styles["Normal"]))
    story.append(Spacer(1, 0.5*cm))

    story.append(Paragraph("一、价格明细", h2_style))
    rows = [
        ["项目", "单价（元/件）", "数量", "金额（元）"],
        ["建议报价", f"{data.suggested_price:.2f}", str(data.quantity), f"{data.suggested_price * data.quantity:.2f}"],
        ["底价参考", f"{data.floor_price:.2f}", "-", "-"],
    ]
    for k, v in data.cost_breakdown.items():
        rows.append([f"成本-{k}", "-", "-", f"{float(v):.2f}"])
    t = Table(rows, colWidths=[4*cm, 4*cm, 3*cm, 4*cm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.grey),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.whitesmoke),
        ("ALIGN", (0, 0), (-1, -1), "CENTER"),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, 0), 10),
        ("BOTTOMPADDING", (0, 0), (-1, 0), 8),
        ("BACKGROUND", (0, 1), (-1, -1), colors.beige),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.black),
    ]))
    story.append(t)
    story.append(Spacer(1, 0.5*cm))

    story.append(Paragraph("二、专业建议", h2_style))
    for line in llm_summary.split("\n"):
        if line.strip():
            story.append(Paragraph(line.strip(), styles["Normal"]))
    story.append(Spacer(1, 0.3*cm))
    story.append(Paragraph(f"报价有效期：{data.valid_days} 天", styles["Normal"]))

    doc.build(story)
    return buffer.getvalue()


async def generate_quotation_pdf(
    data: QuotationDataForPdf,
    *,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
) -> tuple[bytes, str]:
    """
    根据计算出的数据，自动撰写专业 PDF 报价建议书，包含价格明细说明。
    返回 (pdf_bytes, filename)
    """
    api_key = api_key or os.getenv("LLM_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
    base_url = (base_url or os.getenv("LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")).rstrip("/")
    model = model or os.getenv("LLM_MODEL", "qwen-plus")

    prompt = (
        f"请为以下报价数据撰写一段「专业建议」段落（200字内），用于放入 PDF 报价建议书：\n"
        f"产品：{data.product_name}\n"
        f"建议价：{data.suggested_price:.2f} 元/件，数量 {data.quantity}，总金额 {data.suggested_price * data.quantity:.2f} 元\n"
        f"底价：{data.floor_price:.2f} 元/件\n"
        f"成本构成：{data.cost_breakdown}\n\n"
        "要求：突出性价比、交货保障、售后服务等卖点，语气专业诚恳。"
    )
    system = "你是工业品报价专家，擅长撰写专业报价建议书。"

    if api_key:
        try:
            llm_summary = await _call_llm(system, prompt, api_key, base_url, model)
        except Exception:
            llm_summary = "感谢您的询价。我方报价基于成本核算与市场行情，保证质量与交期。"
    else:
        llm_summary = "感谢您的询价。我方报价基于成本核算与市场行情，保证质量与交期。"

    pdf_bytes = _build_pdf_from_data(data, llm_summary)
    filename = f"报价建议书_{data.product_name}_{datetime.now().strftime('%Y%m%d%H%M')}.pdf"
    return pdf_bytes, filename


async def generate_negotiation_strategy(
    req: NegotiationStrategyRequest,
    *,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
) -> NegotiationStrategyResult:
    """
    谈判策略助手：根据客户背景给出针对性谈判话术和最大折扣力度建议。
    """
    api_key = api_key or os.getenv("LLM_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
    base_url = (base_url or os.getenv("LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")).rstrip("/")
    model = model or os.getenv("LLM_MODEL", "qwen-plus")

    margin = (req.suggested_price - req.floor_price) / req.suggested_price if req.suggested_price > 0 else 0
    max_theoretical = min(margin, 0.15)  # 最多让利 15% 或利润率

    prompt = (
        f"客户背景：{req.customer_background}\n"
        f"建议报价：{req.suggested_price:.2f} 元/件，底价：{req.floor_price:.2f} 元/件\n"
        f"产品：{req.product_name or '产品'}\n\n"
        "请输出：\n"
        "1. 针对性谈判话术（3～5 条，每条 1～2 句，针对该客户类型）\n"
        "2. 建议最大折扣力度（0～15% 之间，用数字表示，如 5 表示 5%）\n"
        "3. 谈判要点（3 条关键词）\n"
        "格式：先写话术，空一行，再写「最大折扣：X%」，再写要点列表。"
    )
    system = "你是资深销售谈判专家，擅长根据客户类型制定谈判策略。"

    if not api_key:
        return NegotiationStrategyResult(
            tactics="针对该客户，建议强调长期合作价值与品质保障，可适当给予 3%～5% 的弹性空间。",
            max_discount_pct=0.05,
            key_points=["强调品质", "长期合作", "灵活付款"],
        )

    try:
        content = await _call_llm(system, prompt, api_key, base_url, model)
    except Exception:
        return NegotiationStrategyResult(
            tactics="建议强调品质与交期，可给予 3%～5% 弹性。",
            max_discount_pct=0.05,
            key_points=["品质", "交期", "服务"],
        )

    # 解析最大折扣：优先从 LLM 提取，否则按客户类型启发式
    discount_pct = 0.05
    for line in content.split("\n"):
        if "折扣" in line or "%" in line:
            m = re.search(r"(\d+(?:\.\d+)?)\s*%", line)
            if m:
                discount_pct = min(max_theoretical, float(m.group(1)) / 100)
                break
    # 按客户背景微调
    bg = req.customer_background or ""
    if "长期" in bg or "合作伙伴" in bg:
        discount_pct = min(0.08, max_theoretical)
    elif "价格敏感" in bg:
        discount_pct = min(0.10, max_theoretical)
    elif "急单" in bg:
        discount_pct = min(0.03, max_theoretical)

    return NegotiationStrategyResult(
        tactics=content[:1200] if len(content) > 1200 else content,
        max_discount_pct=round(discount_pct, 4),
        key_points=["品质保障", "交期承诺", "售后服务"],
    )


async def generate_multi_plan_comparison(
    req: MultiPlanComparisonRequest,
    *,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
) -> MultiPlanComparisonResult:
    """
    多方案对比：AI 自动生成经济型、标准型、高端型三个报价方案，
    并分析各方案的利润率和中标概率。
    """
    total_cost = req.material_cost + req.labor_cost + req.energy_cost
    floor = req.floor_price

    # 三档报价
    economy_price = round(floor * 1.05, 2)   # 经济型 +5%
    standard_price = round(floor * 1.12, 2)  # 标准型 +12%
    premium_price = round(floor * 1.22, 2)   # 高端型 +22%

    plans_data = [
        ("economy", "经济型", economy_price),
        ("standard", "标准型", standard_price),
        ("premium", "高端型", premium_price),
    ]

    plans: list[QuotationPlan] = []
    for ptype, pname, price in plans_data:
        revenue = price * req.quantity
        profit = revenue - total_cost
        margin = (profit / revenue) if revenue > 0 else 0
        win_prob = predict_success_rate(price, floor, req.competitor_price, "B")
        plans.append(QuotationPlan(
            plan_type=ptype,
            plan_name=pname,
            unit_price=price,
            total_amount=round(revenue, 2),
            profit_margin=round(margin, 4),
            win_probability=round(win_prob, 4),
            ai_analysis="",  # 由 LLM 填充
        ))

    api_key = api_key or os.getenv("LLM_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
    base_url = (base_url or os.getenv("LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")).rstrip("/")
    model = model or os.getenv("LLM_MODEL", "qwen-plus")

    summary = (
        f"产品：{req.product_name}，数量：{req.quantity}\n"
        f"经济型 {economy_price} 元，利润率 {plans[0].profit_margin*100:.1f}%，中标概率 {plans[0].win_probability*100:.1f}%\n"
        f"标准型 {standard_price} 元，利润率 {plans[1].profit_margin*100:.1f}%，中标概率 {plans[1].win_probability*100:.1f}%\n"
        f"高端型 {premium_price} 元，利润率 {plans[2].profit_margin*100:.1f}%，中标概率 {plans[2].win_probability*100:.1f}%\n\n"
        "请分别用 1 句话分析每个方案的适用场景，并给出整体推荐（选哪个方案及理由）。"
    )
    system = "你是报价策略专家，擅长多方案对比分析。"

    if api_key:
        try:
            analysis = await _call_llm(system, summary, api_key, base_url, model)
            rec = analysis
            parts = [x.strip() for x in analysis.split("\n") if x.strip()]
            plans = [
                QuotationPlan(
                    plan_type=p.plan_type,
                    plan_name=p.plan_name,
                    unit_price=p.unit_price,
                    total_amount=p.total_amount,
                    profit_margin=p.profit_margin,
                    win_probability=p.win_probability,
                    ai_analysis=parts[i][:120] if i < len(parts) else f"{p.plan_name}，利润率{p.profit_margin*100:.1f}%，中标率{p.win_probability*100:.1f}%",
                )
                for i, p in enumerate(plans)
            ]
        except Exception:
            rec = "建议根据客户类型选择：价格敏感选经济型，常规客户选标准型，高端需求选高端型。"
            plans = [
                QuotationPlan(
                    plan_type=p.plan_type,
                    plan_name=p.plan_name,
                    unit_price=p.unit_price,
                    total_amount=p.total_amount,
                    profit_margin=p.profit_margin,
                    win_probability=p.win_probability,
                    ai_analysis=f"{p.plan_name}方案，利润率{p.profit_margin*100:.1f}%，中标概率{p.win_probability*100:.1f}%",
                )
                for p in plans
            ]
    else:
        rec = "建议根据客户类型选择：价格敏感选经济型，常规客户选标准型，高端需求选高端型。"
        plans = [
            QuotationPlan(
                plan_type=p.plan_type,
                plan_name=p.plan_name,
                unit_price=p.unit_price,
                total_amount=p.total_amount,
                profit_margin=p.profit_margin,
                win_probability=p.win_probability,
                ai_analysis=f"{p.plan_name}方案，利润率{p.profit_margin*100:.1f}%，中标概率{p.win_probability*100:.1f}%",
            )
            for p in plans
        ]

    return MultiPlanComparisonResult(plans=plans, recommendation=rec)
