# -*- coding: utf-8 -*-
"""
智能报价服务
计算不亏本的底线价格，支持 iDME 元模型关联（报价单 ↔ 生产批次 ↔ 物料清单）
"""
from typing import Optional
from app.models.schemas import (
    QuotationInput,
    FloorPriceResult,
    BOMItemRealtime,
    QuotationFullInput,
    QuotationFullResult,
)


def _suggested_price_from_market(
    floor_price: float,
    competitor_price: Optional[float],
    customer_level: str,
) -> float:
    """根据市场参数计算建议价"""
    margin_map = {"A": 0.12, "VIP": 0.12, "B": 0.10, "普通": 0.10, "C": 0.08, "新客": 0.08}
    margin = margin_map.get(customer_level.upper() if isinstance(customer_level, str) else "B", 0.10)
    base = floor_price * (1 + margin)
    if competitor_price and competitor_price > 0:
        # 建议价略低于竞品，但不低于底价
        return max(floor_price * 1.02, min(base, competitor_price * 0.98))
    return round(base, 2)


def calculate_floor_price(input_data: QuotationInput) -> FloorPriceResult:
    """
    计算不亏本的底线价格（保本价）。

    公式：floor_price = total_cost / quantity
    total_cost = BOM成本 + 人工成本 + 设备折旧 + 能源消耗 + 物流成本

    输入维度：
    - 原材料实时价格（BOM）
    - 加工工时 × 工时单价 = 人工成本
    - 设备折旧费
    - 能源消耗
    - 物流成本
    """
    # BOM 成本（原材料实时价格汇总）
    bom_cost = sum(
        item.quantity * item.unit_price
        for item in input_data.bom_items
    )

    # 人工成本 = 加工工时 × 工时单价
    labor_cost = input_data.man_hours * input_data.hourly_labor_rate

    # 总成本
    total_cost = (
        bom_cost
        + labor_cost
        + input_data.equipment_depreciation
        + input_data.energy_cost
        + input_data.logistics_cost
    )

    # 底线价格 = 总成本 / 产量（不亏本）
    floor_price = total_cost / input_data.quantity if input_data.quantity > 0 else 0.0

    cost_breakdown = {
        "bom_cost": round(bom_cost, 2),
        "labor_cost": round(labor_cost, 2),
        "equipment_depreciation": round(input_data.equipment_depreciation, 2),
        "energy_cost": round(input_data.energy_cost, 2),
        "logistics_cost": round(input_data.logistics_cost, 2),
    }

    return FloorPriceResult(
        floor_price=round(floor_price, 2),
        total_cost=round(total_cost, 2),
        quantity=input_data.quantity,
        cost_breakdown=cost_breakdown,
    )


def calculate_quotation_full(
    input_data: QuotationFullInput,
    success_rate: float,
    customer_email: Optional[str] = None,
    idme_record_id: Optional[str] = None,
    suggested_deal_price: Optional[float] = None,
    material_price_impact: Optional[dict] = None,
    commodity_prices: Optional[dict] = None,
    correction_applied: Optional[dict] = None,
    green_supply_chain_advice: Optional[dict] = None,
) -> QuotationFullResult:
    """
    完整报价计算：底价、建议价、预计利润率。
    需配合 success_rate_service 和 llm_service 使用。
    """
    total_cost = input_data.material_cost + input_data.labor_cost + input_data.energy_cost
    floor_price = total_cost / input_data.quantity if input_data.quantity > 0 else 0.0
    suggested = (
        correction_applied["corrected_suggested"]
        if correction_applied and "corrected_suggested" in correction_applied
        else _suggested_price_from_market(
            floor_price,
            input_data.competitor_price,
            input_data.customer_level,
        )
    )
    revenue = suggested * input_data.quantity
    profit = revenue - total_cost
    profit_margin = (profit / revenue) if revenue > 0 else 0.0

    # UI 建议：前端 ECharts 展示（以注释形式提供，供人工智能实践赛前端参考）
    cost_breakdown_data = [
        {"name": "材料", "value": input_data.material_cost},
        {"name": "人工", "value": input_data.labor_cost},
        {"name": "能耗", "value": input_data.energy_cost},
    ]
    ui_suggestions = {
        # 价格对比柱状图
        "price_trend_chart": {
            "type": "bar",
            "echarts_option_hint": "xAxis: {type:'category', data:['底价','建议价','竞品价']}, series:[{type:'bar', data:[floor_price,suggested_price,competitor_price||0]}]",
            "data_keys": ["floor_price", "suggested_price", "competitor_price"],
        },
        # 成功率预测仪表盘
        "success_rate_chart": {
            "type": "gauge",
            "echarts_option_hint": "series:[{type:'gauge', min:0, max:100, detail:{formatter:'{value}%'}, data:[{value: success_rate_prediction*100}]}]",
        },
        # 成本构成饼图
        "cost_breakdown_chart": {
            "type": "pie",
            "echarts_option_hint": "series:[{type:'pie', data: cost_breakdown_data}]",
            "data": cost_breakdown_data,
        },
        # 价格-利润率曲线（多档位报价时使用）
        "profit_margin_trend": {
            "type": "line",
            "echarts_option_hint": "xAxis: 报价档位数组, yAxis: 对应利润率, 用于展示「报价从底价到建议价+20%」区间的利润率变化曲线",
        },
    }

    return QuotationFullResult(
        floor_price=round(floor_price, 2),
        suggested_price=round(suggested, 2),
        expected_profit_margin=round(profit_margin, 4),
        success_rate_prediction=round(success_rate, 4),
        customer_email_wording=customer_email,
        idme_record_id=idme_record_id,
        suggested_deal_price=suggested_deal_price,
        material_price_impact=material_price_impact,
        commodity_prices=commodity_prices,
        correction_applied=correction_applied,
        green_supply_chain_advice=green_supply_chain_advice,
        ui_suggestions=ui_suggestions,
    )
