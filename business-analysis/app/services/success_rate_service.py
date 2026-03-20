# -*- coding: utf-8 -*-
"""
成功率预测服务
基于历史中标数据，使用 LinearRegression 预测报价成功率
"""
import numpy as np
from sklearn.linear_model import LinearRegression
from typing import List

from app.models.schemas import HistoricalBidRecord


# 默认历史数据（演示用，实际应从 iDME 或数据库加载）
DEFAULT_HISTORICAL_BIDS: List[HistoricalBidRecord] = [
    HistoricalBidRecord(quoted_price=120, floor_price=80, competitor_price=115, customer_level_rank=1, won=True),
    HistoricalBidRecord(quoted_price=95, floor_price=80, competitor_price=100, customer_level_rank=2, won=True),
    HistoricalBidRecord(quoted_price=110, floor_price=80, competitor_price=105, customer_level_rank=1, won=True),
    HistoricalBidRecord(quoted_price=85, floor_price=80, competitor_price=90, customer_level_rank=3, won=False),
    HistoricalBidRecord(quoted_price=100, floor_price=80, competitor_price=95, customer_level_rank=2, won=True),
    HistoricalBidRecord(quoted_price=130, floor_price=80, competitor_price=120, customer_level_rank=1, won=False),
    HistoricalBidRecord(quoted_price=88, floor_price=80, competitor_price=92, customer_level_rank=2, won=True),
    HistoricalBidRecord(quoted_price=82, floor_price=80, competitor_price=85, customer_level_rank=3, won=True),
    HistoricalBidRecord(quoted_price=105, floor_price=80, competitor_price=110, customer_level_rank=2, won=False),
    HistoricalBidRecord(quoted_price=92, floor_price=80, competitor_price=98, customer_level_rank=2, won=True),
]

_model: LinearRegression | None = None
_trained = False


def retrain_model(historical: List[HistoricalBidRecord]) -> None:
    """使用自定义历史数据重新训练模型（竞赛时可从 iDME 拉取）"""
    global _model, _trained  # noqa: PLW0603
    _model = None
    _trained = False
    _ensure_trained(historical)


def _build_features(
    quoted_price: float,
    floor_price: float,
    competitor_price: float | None,
    customer_level_rank: int,
) -> np.ndarray:
    """构建特征：报价/底价比、与竞品价差、客户等级"""
    price_ratio = quoted_price / floor_price if floor_price > 0 else 1.0
    comp_diff = (quoted_price - (competitor_price or quoted_price)) / max(quoted_price, 1) if competitor_price else 0
    return np.array([[price_ratio, comp_diff, float(customer_level_rank)]])


def _ensure_trained(historical: List[HistoricalBidRecord] | None = None) -> LinearRegression:
    global _model, _trained  # noqa: PLW0603
    if _model is not None and _trained:
        return _model
    records = historical or DEFAULT_HISTORICAL_BIDS
    X = []
    y = []
    for r in records:
        feat = _build_features(
            r.quoted_price, r.floor_price,
            r.competitor_price, r.customer_level_rank,
        )
        X.append(feat[0])
        y.append(1.0 if r.won else 0.0)
    X = np.array(X)
    y = np.array(y)
    _model = LinearRegression()
    _model.fit(X, y)
    _trained = True
    return _model


def predict_success_rate(
    quoted_price: float,
    floor_price: float,
    competitor_price: float | None = None,
    customer_level: str = "B",
    historical: List[HistoricalBidRecord] | None = None,
) -> float:
    """
    基于历史中标数据预测报价成功率。
    使用 LinearRegression，输出裁剪到 [0, 1]。
    """
    level_map = {"A": 1, "VIP": 1, "B": 2, "普通": 2, "C": 3, "新客": 3}
    rank = level_map.get(customer_level.upper() if isinstance(customer_level, str) else "B", 2)
    model = _ensure_trained(historical)
    feat = _build_features(quoted_price, floor_price, competitor_price, rank)
    pred = float(model.predict(feat)[0])
    return max(0.0, min(1.0, pred))
