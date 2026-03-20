# -*- coding: utf-8 -*-
"""
市场进入策略 & 自然语言报告 - LLM 服务
支持智谱（Zhipu）、OpenAI GPT-4、千问等 OpenAI 兼容 API
"""
import os
import httpx
from typing import Optional, Any

from app.models.schemas import (
    CostProfitData,
    MarketData,
    MarketStrategyResponse,
)


def _build_prompt(product_name: str, cost_profit: CostProfitData, market_data: MarketData) -> str:
    """构建 LLM 提示词"""
    direct = cost_profit.bom_cost + cost_profit.labor_cost + cost_profit.energy_cost
    overhead = direct * cost_profit.overhead_rate
    total_cost = direct + overhead + cost_profit.logistics_delay_cost
    revenue = cost_profit.selling_price * cost_profit.quantity
    profit = revenue - total_cost
    margin = (profit / revenue * 100) if revenue > 0 else 0

    lines = [
        f"产品：{product_name}",
        "",
        "【成本与利润】",
        f"- BOM 成本：{cost_profit.bom_cost:.2f} 元",
        f"- 人工成本：{cost_profit.labor_cost:.2f} 元",
        f"- 能耗成本：{cost_profit.energy_cost:.2f} 元",
        f"- 电力波动系数：{cost_profit.power_fluctuation * 100:.1f}%",
        f"- 物流延迟成本：{cost_profit.logistics_delay_cost:.2f} 元",
        f"- 售价：{cost_profit.selling_price:.2f} 元/件",
        f"- 产量：{cost_profit.quantity} 件",
        f"- 总成本：{total_cost:.2f} 元，收入：{revenue:.2f} 元，利润：{profit:.2f} 元，毛利率：{margin:.2f}%",
        "",
        "【市场与用户】",
        f"- 目标行业：{market_data.target_industry}",
        f"- 用户画像：{market_data.user_profile.role}",
        f"- 市场渗透率：{market_data.market_penetration_rate * 100:.1f}%",
    ]
    if market_data.competitor_price is not None:
        lines.append(f"- 竞品价格：{market_data.competitor_price:.2f} 元")
    if market_data.market_size is not None:
        lines.append(f"- 市场规模：{market_data.market_size:.0f} 万元")
    if market_data.user_profile.pain_points:
        lines.append(f"- 用户痛点：{', '.join(market_data.user_profile.pain_points)}")

    lines.extend([
        "",
        "请基于以上数据，输出一份「市场进入策略建议」，包含：",
        "1. 目标客户与用户画像匹配建议",
        "2. 定价策略（考虑竞品与成本）",
        "3. 渠道与推广建议",
        "4. 竞争分析与差异化建议",
        "5. 风险提示与应对措施",
        "使用 Markdown 格式，分点列出，简洁专业。",
    ])
    return "\n".join(lines)


def _build_natural_report_prompt(
    product_name: str,
    cost_profit: CostProfitData,
    market_data: MarketData,
    sensitivity_results: list[dict],
) -> str:
    """构建自然语言报告提示词：将枯燥的图表/数据转化为可读文字"""
    direct = cost_profit.bom_cost + cost_profit.labor_cost + cost_profit.energy_cost
    overhead = direct * cost_profit.overhead_rate
    total_cost = direct + overhead + cost_profit.logistics_delay_cost
    revenue = cost_profit.selling_price * cost_profit.quantity
    profit = revenue - total_cost
    margin = (profit / revenue * 100) if revenue > 0 else 0

    lines = [
        "请将以下商业分析数据转化为一段简洁、可执行的「自然语言报告」，面向管理层阅读。",
        "要求：",
        "1. 用通俗语言概括关键发现，不要罗列数字",
        "2. 结合敏感度分析结果，指出主要风险因素（如原材料、电力、物流）",
        "3. 针对核心客群（用户画像）给出 1～2 条具体可执行建议",
        "4. 若原材料/成本处于高位，可建议租赁方案、分阶段定价等策略以保证利润率",
        "5. 控制在 150～250 字，一段话即可",
        "",
        "【数据摘要】",
        f"产品：{product_name}",
        f"基准利润：{profit:.2f} 元，毛利率：{margin:.2f}%",
        f"目标行业：{market_data.target_industry}，核心客群：{market_data.user_profile.role}",
        "",
        "【敏感度分析结果】",
    ]
    for r in sensitivity_results:
        lines.append(
            f"- {r.get('factor', '')} 变动 {r.get('change_percent', 0)}%："
            f"利润变化 {r.get('profit_change', 0):.2f} 元（{r.get('profit_change_percent', 0):.2f}%）"
        )
    lines.append("")
    lines.append("请直接输出自然语言报告，不要加标题或分点，一段话概括。")
    return "\n".join(lines)


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
    if not choices:
        return ""
    msg = choices[0].get("message", {})
    return msg.get("content", "").strip()


async def generate_market_strategy(
    product_name: str,
    cost_profit: CostProfitData,
    market_data: MarketData,
    *,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
) -> MarketStrategyResponse:
    """
    调用 LLM API 生成市场进入策略建议。
    支持智谱、GPT-4、千问等 OpenAI 兼容接口。
    """
    api_key = api_key or os.getenv("LLM_API_KEY") or os.getenv("OPENAI_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
    base_url = (base_url or os.getenv("LLM_BASE_URL") or
                "https://dashscope.aliyuncs.com/compatible-mode/v1").rstrip("/")
    model = model or os.getenv("LLM_MODEL", "qwen-plus")

    if not api_key:
        return MarketStrategyResponse(
            success=False,
            suggestion="请配置 LLM_API_KEY、OPENAI_API_KEY 或 DASHSCOPE_API_KEY 环境变量",
            product_name=product_name,
        )

    prompt = _build_prompt(product_name, cost_profit, market_data)
    system = "你是一位资深工业品商业分析师，擅长市场进入策略与用户画像分析。输出简洁、可执行的建议。"

    try:
        content = await _call_llm(system, prompt, api_key, base_url, model)
    except Exception as e:
        return MarketStrategyResponse(
            success=False,
            suggestion=f"调用 LLM 失败：{str(e)}",
            product_name=product_name,
        )

    if not content:
        return MarketStrategyResponse(
            success=False,
            suggestion="LLM 未返回有效内容",
            product_name=product_name,
        )

    summary = content[:200] + "..." if len(content) > 200 else content
    return MarketStrategyResponse(
        success=True,
        suggestion=content,
        product_name=product_name,
        summary=summary,
    )


async def generate_natural_language_report(
    product_name: str,
    cost_profit: CostProfitData,
    market_data: MarketData,
    sensitivity_results: list[dict],
    *,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
) -> str:
    """
    将枯燥的图表/数据转化为自然语言报告。
    示例输出：「AI 分析显示：当前原材料价格处于高位，建议针对华南地区的小型电子加工厂
    （核心客群）推出租赁方案以保证利润率。」
    """
    api_key = api_key or os.getenv("LLM_API_KEY") or os.getenv("OPENAI_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
    base_url = (base_url or os.getenv("LLM_BASE_URL") or
                "https://dashscope.aliyuncs.com/compatible-mode/v1").rstrip("/")
    model = model or os.getenv("LLM_MODEL", "qwen-plus")

    if not api_key:
        return "请配置 LLM API Key 以生成自然语言报告。"

    prompt = _build_natural_report_prompt(
        product_name, cost_profit, market_data, sensitivity_results
    )
    system = (
        "你是商业分析报告撰写专家。将数据转化为简洁、可读的自然语言，"
        "面向管理层，突出关键发现和可执行建议。"
    )

    try:
        return await _call_llm(system, prompt, api_key, base_url, model)
    except Exception as e:
        return f"生成报告失败：{str(e)}"


async def generate_product_context_demo(
    product: str,
    *,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
) -> dict:
    """
    根据产品名称生成绿色生产、商业分析的产品专属演示内容。
    用于从产品问答跳转后展示对应产品的绿色/商业分析要点。不依赖 8003。
    """
    api_key = api_key or os.getenv("LLM_API_KEY") or os.getenv("OPENAI_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
    base_url = (base_url or os.getenv("LLM_BASE_URL") or "https://dashscope.aliyuncs.com/compatible-mode/v1").rstrip("/")
    model = model or os.getenv("LLM_MODEL", "qwen-plus")

    green_summary = ""
    business_summary = ""

    if not api_key:
        green_summary = (
            f"针对「{product}」的绿色生产：建议关注生产环节的能效监测（PUE）、碳足迹核算、低碳排产优化，"
            "以及锡焊/注塑等工序的废气回收与环保认证。"
        )
        business_summary = (
            f"针对「{product}」的商业分析：建议关注 BOM 成本、人工与能耗的利润敏感度，"
            "智能报价与中标率预测，以及目标市场的进入策略与产能规划。"
        )
        return {"success": True, "product": product, "green_summary": green_summary, "business_summary": business_summary}

    green_prompt = (
        f"针对「{product}」这款产品，请用 2-3 段话（每段 2-4 句）说明其绿色生产要点，"
        "包括：能效与 PUE、碳足迹、低碳排产、绿色供应链、次品浪费折算、异常检测等。"
        "要具体结合该产品的制造工艺和材料，不要泛泛而谈。直接输出内容，不要标题。"
    )
    biz_prompt = (
        f"针对「{product}」这款产品，请用 2-3 段话（每段 2-4 句）说明其商业分析要点，"
        "包括：利润敏感度、智能报价、市场进入策略、成本结构、最优生产方案等。"
        "要具体结合该产品的行业和供应链特点。直接输出内容，不要标题。"
    )

    try:
        green_summary = await _call_llm("你是工业绿色制造专家。", green_prompt, api_key, base_url, model)
    except Exception:
        green_summary = (
            f"针对「{product}」的绿色生产：建议关注生产环节的能效监测（PUE）、碳足迹核算、低碳排产优化，"
            "以及锡焊/注塑等工序的废气回收与环保认证。"
        )
    try:
        business_summary = await _call_llm("你是工业商业分析专家。", biz_prompt, api_key, base_url, model)
    except Exception:
        business_summary = (
            f"针对「{product}」的商业分析：建议关注 BOM 成本、人工与能耗的利润敏感度，"
            "智能报价与中标率预测，以及目标市场的进入策略与产能规划。"
        )

    return {
        "success": True,
        "product": product,
        "green_summary": green_summary or "（暂无）",
        "business_summary": business_summary or "（暂无）",
    }


async def generate_customer_email_wording(
    product_name: str,
    suggested_price: float,
    floor_price: float,
    profit_margin: float,
    customer_level: str,
    competitor_price: Optional[float] = None,
    *,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
) -> str:
    """
    根据报价结果自动生成「给客户的邮件话术」。
    用于智能报价模块，一键生成可发送的商务邮件正文。
    """
    api_key = api_key or os.getenv("LLM_API_KEY") or os.getenv("OPENAI_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
    base_url = (base_url or os.getenv("LLM_BASE_URL") or
                "https://dashscope.aliyuncs.com/compatible-mode/v1").rstrip("/")
    model = model or os.getenv("LLM_MODEL", "qwen-plus")

    if not api_key:
        return "请配置 LLM API Key 以生成邮件话术。"

    prompt = (
        f"请根据以下报价信息，生成一封发给客户的商务邮件正文（可直接复制发送）：\n\n"
        f"产品：{product_name}\n"
        f"建议报价：{suggested_price:.2f} 元/件\n"
        f"客户等级：{customer_level}\n"
        f"预计利润率：{profit_margin * 100:.1f}%\n"
        + (f"竞品参考价：{competitor_price:.2f} 元/件\n" if competitor_price else "")
        + "\n要求：\n"
        "1. 语气专业、诚恳，突出产品价值与性价比\n"
        "2. 适当体现对客户的重视（根据客户等级调整措辞）\n"
        "3. 包含报价、交货周期、售后服务等关键信息占位\n"
        "4. 200～350 字，不要加标题，直接正文"
    )
    system = "你是工业品销售专员，擅长撰写商务邮件。输出简洁、得体、可直接使用的邮件正文。"

    try:
        return await _call_llm(system, prompt, api_key, base_url, model)
    except Exception as e:
        return f"生成邮件话术失败：{str(e)}"
