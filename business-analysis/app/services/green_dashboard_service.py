# -*- coding: utf-8 -*-
"""
绿色生产仪表盘数据服务
提供减碳趋势图、PUE 趋势等 Dashboard 展示数据
"""
from datetime import datetime, timedelta
from typing import Optional

# 简单内存存储，实际可对接 iDME 或时序数据库
_dashboard_records: list[dict] = []


def record_green_metrics(pue: float, co2_kg: float) -> None:
    """记录当日绿色生产指标（可由能效/碳足迹接口调用时写入）"""
    today = datetime.now().strftime("%Y-%m-%d")
    for r in _dashboard_records:
        if r.get("date") == today:
            r["pue"] = pue
            r["co2_kg"] = co2_kg
            return
    _dashboard_records.append({"date": today, "pue": pue, "co2_kg": co2_kg})
    _dashboard_records.sort(key=lambda x: x["date"])
    if len(_dashboard_records) > 90:
        _dashboard_records.pop(0)


def get_dashboard_data(days: int = 30) -> dict:
    """
    绿色生产仪表盘数据接口。
    返回减碳趋势图、PUE 趋势等，供 Dashboard 展示。
    """
    if not _dashboard_records:
        # 生成模拟趋势数据（演示用）
        base = datetime.now() - timedelta(days=days)
        dates = []
        co2_values = []
        pue_values = []
        co2 = 1200
        pue = 1.8
        for i in range(days):
            d = (base + timedelta(days=i)).strftime("%Y-%m-%d")
            dates.append(d)
            co2 = max(800, co2 - (i % 5) * 8 + (i % 3) * 5)  # 模拟下降趋势
            pue = max(1.2, min(2.0, pue - 0.01 * (i % 4)))
            co2_values.append(round(co2, 2))
            pue_values.append(round(pue, 2))
        return {
            "dates": dates,
            "carbon_trend": co2_values,
            "pue_trend": pue_values,
            "summary": {
                "latest_co2_kg": co2_values[-1] if co2_values else 0,
                "latest_pue": pue_values[-1] if pue_values else 1.5,
                "reduction_pct": round((1 - co2_values[-1] / co2_values[0]) * 100, 1) if co2_values else 0,
            },
            "chart_hint": {
                "carbon_trend": "ECharts 折线图：xAxis=dates, yAxis=carbon_trend, 展示减碳趋势",
                "pue_trend": "ECharts 折线图：xAxis=dates, yAxis=pue_trend, 展示 PUE 能效趋势",
            },
        }

    dates = [r["date"] for r in _dashboard_records[-days:]]
    co2_values = [r["co2_kg"] for r in _dashboard_records[-days:]]
    pue_values = [r["pue"] for r in _dashboard_records[-days:]]

    return {
        "dates": dates,
        "carbon_trend": co2_values,
        "pue_trend": pue_values,
        "summary": {
            "latest_co2_kg": co2_values[-1] if co2_values else 0,
            "latest_pue": pue_values[-1] if pue_values else 1.5,
            "reduction_pct": round((1 - co2_values[-1] / co2_values[0]) * 100, 1) if len(co2_values) > 1 else 0,
        },
        "chart_hint": {
            "carbon_trend": "ECharts 折线图：xAxis=dates, yAxis=carbon_trend, 展示减碳趋势",
            "pue_trend": "ECharts 折线图：xAxis=dates, yAxis=pue_trend, 展示 PUE 能效趋势",
        },
    }
