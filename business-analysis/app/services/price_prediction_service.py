# -*- coding: utf-8 -*-
"""
价格预测模型
调用历史数据，利用回归算法预测未来一个月原材料价格波动对当前报价的影响
"""
import numpy as np
from sklearn.linear_model import LinearRegression
from typing import List

from app.models.schemas import MaterialPriceRecord, PricePredictionResult


def predict_material_price_impact(
    historical_prices: List[MaterialPriceRecord],
    material_cost_share: float,
    current_floor_price: float,
) -> PricePredictionResult:
    """
    基于历史价格序列，用线性回归预测下一期价格变动，
    并计算对当前报价（底价）的影响。

    影响公式：floor_price_impact = current_floor_price * material_cost_share * (predicted_change_pct)
    """
    if len(historical_prices) < 3:
        return PricePredictionResult(
            predicted_material_change_pct=0.0,
            floor_price_impact=0.0,
            suggested_floor_price_adjustment=0.0,
            model_info="历史数据不足，无法预测",
        )

    X = np.array([[r.date_index] for r in historical_prices])
    y = np.array([r.unit_price for r in historical_prices])
    model = LinearRegression()
    model.fit(X, y)

    last_index = max(r.date_index for r in historical_prices)
    next_index = last_index + 1  # 下一期（如下一月）
    predicted_price = float(model.predict(np.array([[next_index]]))[0])
    current_avg = np.mean(y)
    if current_avg <= 0:
        change_pct = 0.0
    else:
        change_pct = (predicted_price - current_avg) / current_avg

    # 对底价的影响：材料成本占比 × 材料价格变动 × 当前底价
    floor_price_impact = current_floor_price * material_cost_share * change_pct
    suggested_adjustment = floor_price_impact

    return PricePredictionResult(
        predicted_material_change_pct=round(change_pct, 4),
        floor_price_impact=round(floor_price_impact, 2),
        suggested_floor_price_adjustment=round(suggested_adjustment, 2),
        model_info="基于 LinearRegression 的历史价格趋势预测",
    )
