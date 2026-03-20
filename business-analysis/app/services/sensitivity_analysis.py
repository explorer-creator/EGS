# -*- coding: utf-8 -*-
"""
利润敏感度分析服务 - 多因子模型
使用 Random Forest，特征包含：BOM、人工、能耗、电力波动、物流延迟
不再仅用 利润=售价-成本，而是考虑电力波动和物流延迟对利润的影响
"""
import numpy as np
from sklearn.ensemble import RandomForestRegressor
from typing import List

from app.models.schemas import (
    CostProfitData,
    SensitivityScenario,
    SensitivityResult,
    ProfitSensitivityResponse,
)

# 特征顺序：[bom_scale, labor_scale, energy_scale, power_fluctuation, logistics_delay_scale]
FACTOR_TO_IDX = {
    "bom_cost": 0,
    "labor_cost": 1,
    "energy_cost": 2,
    "power_fluctuation": 3,
    "logistics_delay": 4,
}


def _compute_profit(
    revenue: float,
    base_bom: float,
    base_labor: float,
    base_energy: float,
    overhead_rate: float,
    base_logistics: float,
    bom_scale: float,
    labor_scale: float,
    energy_scale: float,
    power_fluctuation: float,
    logistics_scale: float,
) -> float:
    """
    计算利润，考虑电力波动和物流延迟：
    - 实际能耗 = 基准能耗 * energy_scale * (1 + power_fluctuation)
    - 物流成本 = 基准物流 * logistics_scale
    """
    direct = base_bom * bom_scale + base_labor * labor_scale + base_energy * energy_scale * (1 + power_fluctuation)
    total_cost = direct * (1 + overhead_rate) + base_logistics * logistics_scale
    return revenue - total_cost


def _compute_base_metrics(cp: CostProfitData) -> tuple[float, float, float]:
    """计算基准收入、总成本、利润（含电力波动、物流延迟）"""
    base_logistics = cp.logistics_delay_cost
    actual_energy = cp.energy_cost * (1 + cp.power_fluctuation)
    direct_cost = cp.bom_cost + cp.labor_cost + actual_energy
    overhead = direct_cost * cp.overhead_rate
    total_cost = direct_cost + overhead + base_logistics
    revenue = cp.selling_price * cp.quantity
    profit = revenue - total_cost
    return revenue, total_cost, profit


def _build_training_data(
    revenue: float,
    base_bom: float,
    base_labor: float,
    base_energy: float,
    overhead_rate: float,
    base_logistics: float,
) -> tuple[np.ndarray, np.ndarray]:
    """
    构建训练数据，特征包含：BOM、人工、能耗、电力波动、物流延迟
    """
    X_list = []
    y_list = []
    for bom_s in np.linspace(0.85, 1.2, 8):
        for labor_s in np.linspace(0.85, 1.2, 6):
            for energy_s in np.linspace(0.85, 1.2, 6):
                for pf in np.linspace(-0.1, 0.2, 5):  # 电力波动 -10% ~ +20%
                    for log_s in np.linspace(0.8, 1.5, 5):  # 物流延迟 0.8x ~ 1.5x
                        profit = _compute_profit(
                            revenue, base_bom, base_labor, base_energy,
                            overhead_rate, base_logistics,
                            bom_s, labor_s, energy_s, pf, log_s,
                        )
                        X_list.append([bom_s, labor_s, energy_s, pf, log_s])
                        y_list.append(profit)
    return np.array(X_list), np.array(y_list)


def run_profit_sensitivity(
    cost_profit: CostProfitData,
    scenarios: List[SensitivityScenario],
) -> ProfitSensitivityResponse:
    """
    利润敏感度分析：多因子 Random Forest 模型。
    特征变量：BOM、人工、能耗、电力波动、物流延迟。
    """
    revenue, total_cost, base_profit = _compute_base_metrics(cost_profit)
    base_bom = cost_profit.bom_cost
    base_labor = cost_profit.labor_cost
    base_energy = cost_profit.energy_cost
    overhead_rate = cost_profit.overhead_rate
    base_logistics = cost_profit.logistics_delay_cost
    base_pf = cost_profit.power_fluctuation

    # 基准物流 scale = 1 时的物流成本
    base_logistics_for_scale = max(base_logistics, 1.0)  # 避免为 0 时无法缩放

    X, y = _build_training_data(
        revenue, base_bom, base_labor, base_energy,
        overhead_rate, base_logistics_for_scale,
    )
    model = RandomForestRegressor(n_estimators=50, max_depth=8, random_state=42)
    model.fit(X, y)

    # 基准输入
    base_input = np.array([[
        1.0, 1.0, 1.0,
        base_pf,
        1.0 if base_logistics > 0 else 1.0,  # logistics_scale
    ]])
    pred_base = float(model.predict(base_input)[0])

    results: List[SensitivityResult] = []
    for s in scenarios:
        idx = FACTOR_TO_IDX.get(s.factor)
        if idx is None:
            continue

        scale = 1 + s.change_percent / 100.0
        inp = base_input.copy()
        if s.factor == "power_fluctuation":
            # power_fluctuation 是绝对值，+10% 表示在基准上增加 0.1
            inp[0, 3] = base_pf + s.change_percent / 100.0
        elif s.factor == "logistics_delay":
            inp[0, 4] = scale
        else:
            inp[0, idx] = scale

        predicted_profit = float(model.predict(inp)[0])
        profit_change = predicted_profit - base_profit
        profit_change_percent = (profit_change / base_profit * 100) if base_profit != 0 else 0

        results.append(SensitivityResult(
            factor=s.factor,
            change_percent=s.change_percent,
            original_profit=round(base_profit, 2),
            predicted_profit=round(predicted_profit, 2),
            profit_change=round(profit_change, 2),
            profit_change_percent=round(profit_change_percent, 2),
        ))

    return ProfitSensitivityResponse(
        base_profit=round(base_profit, 2),
        base_revenue=round(revenue, 2),
        base_total_cost=round(total_cost, 2),
        results=results,
        model_info="基于 Scikit-learn Random Forest 的多因子利润敏感度预测（含电力波动、物流延迟）",
    )
