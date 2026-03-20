# -*- coding: utf-8 -*-
"""
误差补偿与自我修正
AI 自动学习历史报价与实际结标价的偏差，并进行自我修正
体现强化学习/反馈闭环思想：用实际结标结果持续优化报价建议
"""
from typing import List

from app.models.schemas import BidFeedbackRecord


def compute_correction_factor(records: List[BidFeedbackRecord]) -> float:
    """
    根据历史报价与实际结标价的偏差，计算修正系数。
    偏差 = actual_deal_price - quoted_price
    - 若平均偏差为负：常需打折，建议报价上浮以预留空间
    - 若平均偏差为正：常以高于报价成交，可维持或微调
    返回修正系数，1.0 表示无需修正，>1 表示建议上浮，<1 表示建议下浮
    """
    if not records:
        return 1.0

    ratios = []
    for r in records:
        if r.quoted_price > 0:
            # 实际/报价 比值，<1 表示打折，>1 表示溢价
            ratios.append(r.actual_deal_price / r.quoted_price)
    if not ratios:
        return 1.0

    avg_ratio = sum(ratios) / len(ratios)
    # 若平均 0.95，即常打 95 折，则建议报价乘以 1/0.95≈1.053 以抵偿
    # 限制修正幅度在 ±8% 内
    if avg_ratio < 0.5 or avg_ratio > 1.5:
        return 1.0
    correction = 1.0 / avg_ratio
    return round(max(0.92, min(1.08, correction)), 4)


def apply_correction(suggested_price: float, correction_factor: float) -> float:
    """将修正系数应用到建议价"""
    return round(suggested_price * correction_factor, 2)


def get_correction_summary(records: List[BidFeedbackRecord]) -> dict:
    """返回误差补偿分析摘要（用于展示）"""
    if not records:
        return {"factor": 1.0, "message": "暂无历史结标数据，未启用自我修正"}
    factor = compute_correction_factor(records)
    avg_dev = sum(r.actual_deal_price - r.quoted_price for r in records) / len(records)
    return {
        "correction_factor": factor,
        "avg_deviation": round(avg_dev, 2),
        "sample_count": len(records),
        "message": f"基于 {len(records)} 条历史结标数据，AI 自动学习偏差并修正，建议系数 {factor}",
    }
