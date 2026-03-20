# -*- coding: utf-8 -*-
"""
低碳优化算法
利用遗传算法在保证交付时间的前提下，自动调整设备启停顺序，
最大化利用绿电（谷时）或降低总能耗。
"""
import random
from typing import List

from app.models.schemas import (
    LowCarbonScheduleRequest,
    LowCarbonScheduleResult,
    ScheduleOrder,
    ScheduledTask,
    ElectricityPrice,
)


# 峰谷平时段（小时 0-24）
# 谷时：0-6, 23  峰时：8-11, 18-21  平时：其余
def _get_slot_type(hour: float) -> str:
    h = int(hour) % 24
    if h < 7 or h >= 23:
        return "valley"
    if (8 <= h < 12) or (18 <= h < 22):
        return "peak"
    return "flat"


def _get_price(hour: float, price: ElectricityPrice) -> float:
    t = _get_slot_type(hour)
    if t == "peak":
        return price.peak
    if t == "valley":
        return price.valley
    return price.flat


def _compute_schedule_cost(
    orders: List[ScheduleOrder],
    ordering: List[int],
    price: ElectricityPrice,
    idle_kw: float,
    work_start: float,
) -> tuple[float, float, List[ScheduledTask], float]:
    """
    计算给定排序下的总能耗(kWh)、总电费(元)、任务列表、以及违约惩罚
    """
    tasks: List[ScheduledTask] = []
    total_kwh = 0.0
    total_cost = 0.0
    penalty = 0.0
    current_hour = work_start

    for idx in ordering:
        o = orders[idx]
        start = current_hour
        end = current_hour + o.duration_hours

        # 按小时累计能耗与电费
        kwh = 0.0
        cost = 0.0
        h = start
        while h < end:
            step = min(1.0, end - h)
            kwh += o.power_kw * step
            cost += o.power_kw * step * _get_price(h, price)
            h += step

        total_kwh += kwh
        total_cost += cost

        # 违约惩罚：完成时间超过 deadline
        if end > o.deadline_hours:
            penalty += (end - o.deadline_hours) * 10000

        tasks.append(ScheduledTask(
            order_id=o.order_id,
            start_hour=round(start, 2),
            end_hour=round(end, 2),
            time_slot_type=_get_slot_type(start),
            energy_kwh=round(kwh, 2),
            energy_cost=round(cost, 2),
        ))
        current_hour = end

    # 若有空闲时段（如当日结束后到次日），可加空转能耗；此处简化
    return total_kwh, total_cost, tasks, penalty


def _traditional_ordering(orders: List[ScheduleOrder]) -> List[int]:
    """传统排产：按紧急程度降序、交付期限升序"""
    indexed = [(i, o) for i, o in enumerate(orders)]
    indexed.sort(key=lambda x: (-x[1].urgency, x[1].deadline_hours))
    return [x[0] for x in indexed]


def _genetic_optimize(
    orders: List[ScheduleOrder],
    price: ElectricityPrice,
    idle_kw: float,
    work_start: float,
    pop_size: int = 50,
    generations: int = 80,
) -> List[int]:
    """遗传算法优化排序"""
    n = len(orders)
    if n <= 1:
        return list(range(n))

    def fitness(ordering: List[int]) -> float:
        _, cost, _, penalty = _compute_schedule_cost(orders, ordering, price, idle_kw, work_start)
        return -(cost + penalty)

    # 初始种群：传统 + 随机
    population: List[List[int]] = [_traditional_ordering(orders)]
    for _ in range(pop_size - 1):
        ind = list(range(n))
        random.shuffle(ind)
        population.append(ind)

    for _ in range(generations):
        # 评估
        scored = [(fitness(p), p) for p in population]
        scored.sort(key=lambda x: x[0], reverse=True)

        # 选择前 50%
        survivors = [p for _, p in scored[: pop_size // 2]]

        # 交叉与变异
        while len(population) < pop_size:
            p1, p2 = random.sample(survivors, 2)
            # 顺序交叉 OX
            a, b = sorted(random.sample(range(n), 2))
            segment = set(p1[a:b])
            rest = [x for x in p2 if x not in segment]
            child = rest[0:a] + p1[a:b] + rest[a : n - (b - a)]

            # 变异：随机交换
            if random.random() < 0.25:
                i, j = random.sample(range(n), 2)
                child[i], child[j] = child[j], child[i]

            population.append(child)

        population = survivors + population[len(survivors) : pop_size]

    best = max(population, key=fitness)
    return best


def compute_low_carbon_schedule(req: LowCarbonScheduleRequest) -> LowCarbonScheduleResult:
    """
    低碳优化排产：遗传算法求解绿色排产计划。
    在保证交付时间的前提下，最大化利用谷时或降低总能耗。
    """
    if not req.orders:
        return LowCarbonScheduleResult(
            schedule=[],
            total_energy_kwh=0,
            total_energy_cost=0,
            traditional_energy_kwh=0,
            traditional_energy_cost=0,
            savings_pct=0,
            algorithm="genetic",
        )

    price = req.electricity_price
    idle_kw = req.idle_energy_kw
    work_start = req.work_start_hour

    # 传统排产
    trad_order = _traditional_ordering(req.orders)
    trad_kwh, trad_cost, _, _ = _compute_schedule_cost(
        req.orders, trad_order, price, idle_kw, work_start
    )

    # 遗传算法优化
    opt_order = _genetic_optimize(
        req.orders, price, idle_kw, work_start,
        pop_size=40,
        generations=60,
    )
    opt_kwh, opt_cost, tasks, penalty = _compute_schedule_cost(
        req.orders, opt_order, price, idle_kw, work_start
    )

    # 若优化方案违约严重，退回传统
    if penalty > opt_cost:
        opt_order = trad_order
        opt_kwh, opt_cost, tasks, _ = _compute_schedule_cost(
            req.orders, opt_order, price, idle_kw, work_start
        )

    # 节省百分比（电能与电费）
    savings_kwh = (1 - opt_kwh / trad_kwh) * 100 if trad_kwh > 0 else 0
    savings_cost = (1 - opt_cost / trad_cost) * 100 if trad_cost > 0 else 0
    savings_pct = round((savings_kwh + savings_cost) / 2, 2)  # 综合

    return LowCarbonScheduleResult(
        schedule=tasks,
        total_energy_kwh=round(opt_kwh, 2),
        total_energy_cost=round(opt_cost, 2),
        traditional_energy_kwh=round(trad_kwh, 2),
        traditional_energy_cost=round(trad_cost, 2),
        savings_pct=savings_pct,
        algorithm="genetic",
    )
