# -*- coding: utf-8 -*-
"""
智能质检 × 环保指标
- 视觉检测出次品时，自动折算物料浪费与能耗浪费
- 异常检测算法：设备故障苗头时提前停机，零缺陷零浪费
"""
import numpy as np
from sklearn.ensemble import IsolationForest

from app.models.schemas import (
    DefectWasteRequest,
    DefectWasteResult,
    AnomalyDetectionRequest,
    AnomalyDetectionResult,
)

# 物料碳排放因子 kg CO2/kg（用于浪费折算）
MATERIAL_CO2_FACTOR = {"copper": 3.0, "tin": 5.0, "fr4": 2.5, "default": 2.0}
GRID_CO2_FACTOR = 0.55


def compute_defect_waste(req: DefectWasteRequest) -> DefectWasteResult:
    """
    每当视觉模型检测出次品，自动折算该次品造成的物料浪费与能耗浪费。
    如：浪费了多少克铜、多少度电、对应 CO2。
    """
    if req.defect_count <= 0:
        return DefectWasteResult(
            defect_count=0,
            material_waste_g={},
            total_material_waste_g=0,
            energy_waste_kwh=0,
            co2_waste_kg=0,
            summary="无次品，零浪费",
        )

    material_waste_g: dict = {}
    total_material_g = 0.0
    for k, v in req.material_per_unit_g.items():
        w = req.defect_count * float(v)
        material_waste_g[k] = round(w, 2)
        total_material_g += w

    energy_waste_kwh = req.defect_count * req.energy_per_unit_kwh

    # CO2：物料浪费 + 能耗浪费
    material_co2 = 0.0
    for k, v in material_waste_g.items():
        factor = MATERIAL_CO2_FACTOR.get(k.lower(), MATERIAL_CO2_FACTOR["default"])
        material_co2 += (v / 1000) * factor  # g -> kg
    energy_co2 = energy_waste_kwh * GRID_CO2_FACTOR
    co2_waste = material_co2 + energy_co2

    parts = [f"{k} {v}g" for k, v in material_waste_g.items()]
    summary = (
        f"检测到 {req.defect_count} 件次品，浪费："
        f"物料 {', '.join(parts)}，"
        f"电能 {round(energy_waste_kwh, 2)} kWh，"
        f"等效 CO2 {round(co2_waste, 2)} kg"
    )

    return DefectWasteResult(
        defect_count=req.defect_count,
        material_waste_g=material_waste_g,
        total_material_waste_g=round(total_material_g, 2),
        energy_waste_kwh=round(energy_waste_kwh, 2),
        co2_waste_kg=round(co2_waste, 2),
        summary=summary,
    )


def run_anomaly_detection(req: AnomalyDetectionRequest) -> AnomalyDetectionResult:
    """
    异常检测算法：在设备出现故障苗头时提前预警，建议停机，防止大批量废品。
    使用 Isolation Forest 实现。
    """
    if not req.metrics or not req.metric_keys:
        return AnomalyDetectionResult(
            anomaly_score=0,
            recommend_shutdown=False,
            message="无数据",
        )

    # 构建特征矩阵
    X = []
    for m in req.metrics:
        row = []
        for k in req.metric_keys:
            v = m.get(k, 0)
            row.append(float(v) if v is not None else 0)
        X.append(row)

    X = np.array(X)
    if len(X) < 3:
        return AnomalyDetectionResult(
            anomaly_score=0,
            recommend_shutdown=False,
            message="数据点不足，需至少 3 个",
        )

    model = IsolationForest(contamination=0.1, random_state=42)
    pred = model.fit_predict(X)  # -1 = anomaly
    scores = model.decision_function(X)  # 越负越异常

    anomaly_indices = [i for i, p in enumerate(pred) if p == -1]
    # 归一化到 0~1，score 越负越异常
    score_min, score_max = scores.min(), scores.max()
    if score_max > score_min:
        norm_scores = (scores - score_min) / (score_max - score_min)
        anomaly_score = 1 - norm_scores.mean()  # 平均异常程度
    else:
        anomaly_score = 0.5

    # 异常点占比高或最近连续异常 → 建议停机
    ratio = len(anomaly_indices) / len(X)
    recent_anomaly = anomaly_indices and (len(X) - 1) in anomaly_indices
    recommend_shutdown = ratio >= 0.3 or (ratio >= 0.15 and recent_anomaly)

    msg = (
        f"检测到 {len(anomaly_indices)} 个异常点，异常率 {ratio*100:.1f}%。"
        + ("建议提前停机检查，防止大批量废品。" if recommend_shutdown else "持续监控。")
    )

    return AnomalyDetectionResult(
        anomaly_score=round(float(anomaly_score), 4),
        recommend_shutdown=recommend_shutdown,
        anomaly_indices=anomaly_indices[:20],
        message=msg,
    )
