# -*- coding: utf-8 -*-
"""
利润优化算法
根据工厂闲置产能自动推荐建议成交价：
- 产能利用率低 → 调低报价以抢单
- 产能饱和 → 提高溢价
"""
from app.models.schemas import ProfitOptimizationResult


def optimize_deal_price(
    base_price: float,
    capacity_utilization: float,
    floor_price: float,
) -> ProfitOptimizationResult:
    """
    根据产能利用率推荐建议成交价。

    策略：
    - 产能利用率 < 0.5：抢单策略，降价 3%~8%
    - 产能利用率 0.5~0.8：平衡策略，维持或微调
    - 产能利用率 > 0.8：溢价策略，提价 2%~6%
    """
    if capacity_utilization < 0.5:
        # 闲置多，降价抢单
        discount = 0.03 + (0.5 - capacity_utilization) * 0.1  # 3%~8%
        discount = min(0.08, discount)
        adjusted = base_price * (1 - discount)
        strategy = "产能闲置，建议降价抢单"
    elif capacity_utilization < 0.8:
        # 平衡
        adjusted = base_price
        strategy = "产能适中，维持基准报价"
    else:
        # 饱和，溢价
        premium = 0.02 + (capacity_utilization - 0.8) * 0.2  # 2%~6%
        premium = min(0.06, premium)
        adjusted = base_price * (1 + premium)
        strategy = "产能饱和，建议提高溢价"

    # 不低于底价
    adjusted = max(adjusted, floor_price * 1.01)
    adjustment_pct = (adjusted - base_price) / base_price if base_price > 0 else 0

    return ProfitOptimizationResult(
        suggested_deal_price=round(adjusted, 2),
        adjustment_pct=round(adjustment_pct, 4),
        strategy=strategy,
    )
