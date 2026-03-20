# -*- coding: utf-8 -*-
"""
商业决策智能分析 - 数据模型定义
符合工业软件规范，预留与 iDME 数据底座对接的扩展字段
"""
from __future__ import annotations

from typing import Optional
from pydantic import BaseModel, Field


# ============== 成本与利润 ==============

class BOMItem(BaseModel):
    """BOM 物料项（可对接 iDME Material_BOM）"""
    material_id: Optional[str] = Field(None, description="物料ID，iDME 关联用")
    material_name: str = Field(..., description="物料名称")
    quantity: float = Field(1.0, ge=0, description="用量")
    unit_price: float = Field(0.0, ge=0, description="单价（元）")
    unit: str = Field("件", description="单位")


class CostProfitData(BaseModel):
    """成本与利润数据结构"""
    # 生产成本（BOM）
    bom_cost: float = Field(0.0, ge=0, description="BOM 物料总成本（元）")
    bom_items: Optional[list[BOMItem]] = Field(None, description="BOM 明细，可选")
    # 人工
    labor_cost: float = Field(0.0, ge=0, description="人工成本（元）")
    # 能耗
    energy_cost: float = Field(0.0, ge=0, description="能耗成本（元）")
    # 售价
    selling_price: float = Field(0.0, ge=0, description="售价（元/件）")
    # 产量
    quantity: int = Field(1, gt=0, description="产量（件）")
    # 制造费用率（0~1）
    overhead_rate: float = Field(0.15, ge=0, le=1, description="制造费用率")
    # 多因子：电力波动、物流延迟
    power_fluctuation: float = Field(
        0.0, ge=-0.5, le=0.5,
        description="电力波动系数（-0.5~0.5），如 0.1 表示电价上涨 10%"
    )
    logistics_delay_cost: float = Field(
        0.0, ge=0,
        description="物流延迟导致的额外成本（元），如仓储、加急费等"
    )


# ============== 市场与用户 ==============

class UserProfile(BaseModel):
    """用户画像"""
    role: str = Field(..., description="角色，如：电子厂采购、中试车间主任")
    industry: Optional[str] = Field(None, description="所属行业")
    decision_power: Optional[str] = Field(None, description="决策权级")
    pain_points: Optional[list[str]] = Field(None, description="痛点列表")


class MarketData(BaseModel):
    """市场数据"""
    target_industry: str = Field(..., description="目标行业")
    user_profile: UserProfile = Field(..., description="用户画像")
    market_penetration_rate: float = Field(0.0, ge=0, le=1, description="市场渗透率（0~1）")
    competitor_price: Optional[float] = Field(None, ge=0, description="竞品价格（元）")
    market_size: Optional[float] = Field(None, ge=0, description="市场规模（万元）")


# ============== 分析请求/响应 ==============

class BusinessAnalysisInput(BaseModel):
    """商业分析完整输入"""
    product_name: str = Field(..., description="产品名称")
    cost_profit: CostProfitData = Field(..., description="成本与利润数据")
    market_data: MarketData = Field(..., description="市场与用户数据")


class SensitivityScenario(BaseModel):
    """敏感度分析场景"""
    factor: str = Field(
        ...,
        description="变动因素：bom_cost, labor_cost, energy_cost, power_fluctuation, logistics_delay"
    )
    change_percent: float = Field(..., description="变动百分比，如 5 表示 +5%")


class ProfitSensitivityRequest(BaseModel):
    """利润敏感度分析请求"""
    cost_profit: CostProfitData = Field(..., description="成本与利润数据")
    scenarios: list[SensitivityScenario] = Field(
        default_factory=lambda: [
            SensitivityScenario(factor="bom_cost", change_percent=5),
            SensitivityScenario(factor="labor_cost", change_percent=5),
            SensitivityScenario(factor="energy_cost", change_percent=10),
            SensitivityScenario(factor="power_fluctuation", change_percent=10),
            SensitivityScenario(factor="logistics_delay", change_percent=20),
        ],
        description="敏感度分析场景列表"
    )


class SensitivityResult(BaseModel):
    """单个敏感度分析结果"""
    factor: str
    change_percent: float
    original_profit: float
    predicted_profit: float
    profit_change: float
    profit_change_percent: float


class ProfitSensitivityResponse(BaseModel):
    """利润敏感度分析响应"""
    success: bool = True
    base_profit: float = Field(..., description="基准利润")
    base_revenue: float = Field(..., description="基准收入")
    base_total_cost: float = Field(..., description="基准总成本")
    results: list[SensitivityResult] = Field(..., description="各场景预测结果")
    model_info: Optional[str] = Field(None, description="模型说明")


class MarketStrategyRequest(BaseModel):
    """市场进入策略请求"""
    product_name: str = Field(..., description="产品名称")
    cost_profit: CostProfitData = Field(..., description="成本与利润数据")
    market_data: MarketData = Field(..., description="市场与用户数据")


class MarketStrategyResponse(BaseModel):
    """市场进入策略响应"""
    success: bool = True
    suggestion: str = Field(..., description="AI 生成的市场进入策略建议")
    product_name: str = Field(..., description="产品名称")
    summary: Optional[str] = Field(None, description="简要摘要")


class NaturalReportRequest(BaseModel):
    """自然语言报告请求"""
    product_name: str = Field(..., description="产品名称")
    cost_profit: CostProfitData = Field(..., description="成本与利润数据")
    market_data: MarketData = Field(..., description="市场与用户数据")
    sensitivity_results: Optional[list[dict]] = Field(None, description="敏感度分析结果摘要")


class NaturalReportResponse(BaseModel):
    """自然语言报告响应"""
    success: bool = True
    report: str = Field(..., description="AI 生成的自然语言分析报告")
    product_name: str = Field(..., description="产品名称")


class FullAnalysisResponse(BaseModel):
    """完整分析响应（预测数值 + AI 建议 + 自然语言报告）"""
    success: bool = True
    product_name: str
    sensitivity: ProfitSensitivityResponse
    market_strategy: MarketStrategyResponse
    natural_report: Optional[str] = Field(None, description="AI 生成的自然语言报告")


# ============== 智能报价 ==============

class BOMItemRealtime(BaseModel):
    """BOM 物料项（含原材料实时价格，可对接 iDME Material_BOM）"""
    material_id: Optional[str] = Field(None, description="物料ID，iDME 关联")
    material_name: str = Field(..., description="物料名称")
    quantity: float = Field(1.0, ge=0, description="用量")
    unit_price: float = Field(0.0, ge=0, description="实时单价（元）")
    unit: str = Field("件", description="单位")


class QuotationInput(BaseModel):
    """
    智能报价输入维度
    通过 iDME 元模型思路，与生产批次、物料清单关联
    """
    # 原材料实时价格（BOM）
    bom_items: list[BOMItemRealtime] = Field(..., description="物料清单，含实时价格")
    # 加工工时（Man-hours）
    man_hours: float = Field(0.0, ge=0, description="加工工时（小时）")
    hourly_labor_rate: float = Field(50.0, ge=0, description="工时单价（元/小时）")
    # 设备折旧费
    equipment_depreciation: float = Field(0.0, ge=0, description="设备折旧费（元/批次）")
    # 能源消耗
    energy_cost: float = Field(0.0, ge=0, description="能源消耗成本（元）")
    # 物流成本
    logistics_cost: float = Field(0.0, ge=0, description="物流成本（元）")
    # 产量（用于计算单位底线价）
    quantity: int = Field(1, gt=0, description="生产批次产量（件）")
    # iDME 关联（元模型）
    batch_id: Optional[str] = Field(None, description="生产批次ID，iDME WorkingPlan 等")
    bom_id: Optional[str] = Field(None, description="物料清单ID，iDME Material_BOM 关联")
    product_id: Optional[str] = Field(None, description="产品/物料主数据ID")


class FloorPriceResult(BaseModel):
    """底线价格计算结果"""
    floor_price: float = Field(..., description="不亏本底线价格（元/件）")
    total_cost: float = Field(..., description="总成本（元）")
    quantity: int = Field(..., description="产量（件）")
    cost_breakdown: dict = Field(..., description="成本构成明细")


# ============== 智能报价完整（含市场参数） ==============

class BidFeedbackRecord(BaseModel):
    """报价反馈记录：用于 AI 自动学习历史报价与实际结标价的偏差，并进行自我修正"""
    quoted_price: float = Field(..., ge=0, description="当时报价（元/件）")
    actual_deal_price: float = Field(..., ge=0, description="实际结标价（元/件）")


class QuotationFullInput(BaseModel):
    """
    智能报价完整输入：基础成本 + 市场参数
    """
    # 基础成本
    material_cost: float = Field(0.0, ge=0, description="材料成本（元）")
    labor_cost: float = Field(0.0, ge=0, description="人工成本（元）")
    energy_cost: float = Field(0.0, ge=0, description="能耗成本（元）")
    quantity: int = Field(1, gt=0, description="产量（件）")
    # 市场参数
    competitor_price: Optional[float] = Field(None, ge=0, description="竞争对手价（元/件）")
    customer_level: str = Field("B", description="客户等级：A/VIP、B/普通、C/新客")
    product_name: Optional[str] = Field(None, description="产品名称")
    # AI 子模块：产能利用率（利润优化）、历史价格（价格预测）
    capacity_utilization: Optional[float] = Field(None, ge=0, le=1, description="产能利用率（0~1），用于利润优化")
    historical_material_prices: Optional[list[float]] = Field(None, description="历史原材料价格序列（用于价格预测）")
    material_cost_share: Optional[float] = Field(None, ge=0, le=1, description="材料成本占比（0~1），默认按 material_cost/total_cost")
    # 误差补偿：历史报价与实际结标价，用于 AI 自我修正（强化学习/反馈闭环）
    feedback_records: Optional[list[BidFeedbackRecord]] = Field(None, description="历史结标反馈")
    # iDME 关联
    batch_id: Optional[str] = Field(None, description="生产批次ID")
    bom_id: Optional[str] = Field(None, description="物料清单ID")
    # 绿色供应链：可选供应商列表，用于报价时综合考虑价格与碳强度
    suppliers: Optional[list["SupplierInput"]] = Field(None, description="候选供应商，用于绿色采购建议")


class HistoricalBidRecord(BaseModel):
    """历史中标数据（用于成功率预测训练）"""
    quoted_price: float = Field(..., ge=0, description="报价（元/件）")
    floor_price: float = Field(..., ge=0, description="底价（元/件）")
    competitor_price: Optional[float] = Field(None, ge=0)
    customer_level_rank: int = Field(1, description="客户等级：1=A, 2=B, 3=C")
    won: bool = Field(..., description="是否中标")


class QuotationFullResult(BaseModel):
    """智能报价完整结果（逻辑闭环）"""
    floor_price: float = Field(..., description="底价（元/件）")
    suggested_price: float = Field(..., description="建议价（元/件）")
    expected_profit_margin: float = Field(..., description="预计利润率（0~1）")
    success_rate_prediction: float = Field(..., description="成功率预测（0~1）")
    customer_email_wording: Optional[str] = Field(None, description="给客户的邮件话术")
    idme_record_id: Optional[str] = Field(None, description="iDME 写入记录ID")
    # AI 子模块输出
    suggested_deal_price: Optional[float] = Field(None, description="利润优化推荐成交价（考虑产能）")
    material_price_impact: Optional[dict] = Field(None, description="原材料价格预测对报价的影响")
    commodity_prices: Optional[dict] = Field(None, description="大宗商品实时市价（铜、铝）")
    correction_applied: Optional[dict] = Field(None, description="误差补偿自我修正结果")
    # 绿色供应链：便宜但排碳高时黄色预警，推荐更环保替代
    green_supply_chain_advice: Optional[dict] = Field(None, description="绿色采购建议、黄色预警、替代方案")
    # UI 建议：前端展示用
    ui_suggestions: Optional[dict] = Field(None, description="前端图表展示建议（ECharts 等）")


# ============== 价格预测 & 利润优化 ==============

class MaterialPriceRecord(BaseModel):
    """历史原材料价格记录（用于回归预测）"""
    date_index: int = Field(..., description="日期索引，如 1,2,3... 或 202401,202402")
    unit_price: float = Field(..., ge=0, description="单价（元）")
    material_id: Optional[str] = Field(None, description="物料ID")


class PricePredictionRequest(BaseModel):
    """价格预测请求"""
    material_cost_share: float = Field(0.5, ge=0, le=1, description="材料成本在总成本中的占比")
    current_floor_price: float = Field(..., ge=0, description="当前底价（元/件）")
    historical_prices: list[MaterialPriceRecord] = Field(..., description="历史价格序列")


class PricePredictionResult(BaseModel):
    """价格预测结果：未来一个月原材料波动对报价的影响"""
    predicted_material_change_pct: float = Field(..., description="预测材料价格变动百分比")
    floor_price_impact: float = Field(..., description="对底价的影响（元/件）")
    suggested_floor_price_adjustment: float = Field(..., description="建议底价调整量")
    model_info: str = Field("", description="模型说明")


class ProfitOptimizationRequest(BaseModel):
    """利润优化请求"""
    base_price: float = Field(..., ge=0, description="基准价格（建议价或底价）")
    capacity_utilization: float = Field(..., ge=0, le=1, description="产能利用率（0~1）")
    floor_price: float = Field(..., ge=0, description="底价（元/件）")


class ProfitOptimizationResult(BaseModel):
    """利润优化结果：建议成交价"""
    suggested_deal_price: float = Field(..., description="建议成交价（元/件）")
    adjustment_pct: float = Field(..., description="相对基准的调整幅度（正=溢价，负=降价）")
    strategy: str = Field(..., description="策略说明：抢单/平衡/溢价")


# ============== LLM 报价增强 ==============

class QuotationDataForPdf(BaseModel):
    """报价数据（用于 PDF 生成）"""
    product_name: str = Field("产品", description="产品名称")
    floor_price: float = Field(0.0, ge=0)
    suggested_price: float = Field(0.0, ge=0)
    quantity: int = Field(1, gt=0)
    cost_breakdown: dict = Field(default_factory=dict, description="成本明细")
    valid_days: int = Field(30, description="报价有效期（天）")


class NegotiationStrategyRequest(BaseModel):
    """谈判策略助手请求"""
    customer_background: str = Field(..., description="客户背景，如：长期合作伙伴、价格敏感型、急单")
    suggested_price: float = Field(..., ge=0)
    floor_price: float = Field(..., ge=0)
    product_name: Optional[str] = Field(None)


class NegotiationStrategyResult(BaseModel):
    """谈判策略助手结果"""
    tactics: str = Field(..., description="针对性谈判话术")
    max_discount_pct: float = Field(..., description="建议最大折扣力度（0~1，如 0.05 表示 5%）")
    key_points: list[str] = Field(default_factory=list, description="谈判要点")


class QuotationPlan(BaseModel):
    """单个报价方案"""
    plan_type: str = Field(..., description="economy/standard/premium")
    plan_name: str = Field(..., description="经济型/标准型/高端型")
    unit_price: float = Field(..., ge=0)
    total_amount: float = Field(..., ge=0)
    profit_margin: float = Field(..., description="利润率 0~1")
    win_probability: float = Field(..., description="中标概率 0~1")
    ai_analysis: str = Field(..., description="AI 方案分析")


class MultiPlanComparisonRequest(BaseModel):
    """多方案对比请求"""
    product_name: str = Field("产品")
    floor_price: float = Field(..., ge=0)
    quantity: int = Field(1, gt=0)
    material_cost: float = Field(0.0, ge=0)
    labor_cost: float = Field(0.0, ge=0)
    energy_cost: float = Field(0.0, ge=0)
    competitor_price: Optional[float] = Field(None, ge=0)


class MultiPlanComparisonResult(BaseModel):
    """多方案对比结果"""
    plans: list[QuotationPlan] = Field(..., description="三个方案")
    recommendation: str = Field(..., description="AI 推荐说明")


# ============== 绿色生产 ==============

class DevicePowerData(BaseModel):
    """设备实时功率数据"""
    device_id: str = Field(..., description="设备ID")
    device_name: Optional[str] = Field(None)
    power_kw: float = Field(..., ge=0, description="实时功率（kW）")
    device_type: str = Field("process", description="process=生产设备, auxiliary=辅助设备(制冷/照明等)")


class EnergyMonitoringRequest(BaseModel):
    """能效监测请求"""
    devices: list[DevicePowerData] = Field(..., description="设备实时功率数据")
    facility_total_kw: Optional[float] = Field(None, ge=0, description="设施总功率，不填则按设备汇总")


class PUEResult(BaseModel):
    """PUE 能效监测结果"""
    pue: float = Field(..., description="PUE 能源效率指标")
    total_power_kw: float = Field(..., description="总功率 kW")
    process_power_kw: float = Field(..., description="生产设备功率 kW")
    auxiliary_power_kw: float = Field(..., description="辅助设备功率 kW")
    device_count: int = Field(..., description="设备数量")


class CarbonFootprintRequest(BaseModel):
    """碳足迹计算请求"""
    production_hours: float = Field(0.0, ge=0, description="生产工时（小时）")
    avg_power_kw: float = Field(0.0, ge=0, description="平均功率（kW）")
    material_weight_kg: float = Field(0.0, ge=0, description="材料重量（kg）")
    material_type: str = Field("steel", description="材料类型：steel/aluminum/plastic/copper")


class CarbonFootprintResult(BaseModel):
    """碳足迹计算结果"""
    total_co2_kg: float = Field(..., description="总 CO2 排放（kg）")
    energy_co2_kg: float = Field(..., description="能耗产生 CO2（kg）")
    material_co2_kg: float = Field(..., description="材料产生 CO2（kg）")
    breakdown: dict = Field(default_factory=dict, description="明细")


class EnergyOptimizationRequest(BaseModel):
    """节能降碳 AI 报告请求"""
    energy_distribution: dict = Field(..., description="能耗分布，如 {process: 120, auxiliary: 80, lighting: 20}")
    pue: float = Field(1.0, ge=0)
    total_co2_kg: Optional[float] = Field(None, ge=0)


# ============== 低碳优化排产 ==============

class ElectricityPrice(BaseModel):
    """实时电价（峰谷平）元/kWh"""
    peak: float = Field(1.2, ge=0, description="峰时电价")
    valley: float = Field(0.4, ge=0, description="谷时电价")
    flat: float = Field(0.8, ge=0, description="平时电价")


class ScheduleOrder(BaseModel):
    """排产订单"""
    order_id: str = Field(..., description="订单ID")
    duration_hours: float = Field(..., gt=0, description="生产工时（h）")
    urgency: int = Field(3, ge=1, le=5, description="紧急程度 1-5，5 最急")
    deadline_hours: float = Field(24.0, gt=0, description="距交付剩余小时数")
    power_kw: float = Field(50.0, ge=0, description="生产时功率 kW")


class LowCarbonScheduleRequest(BaseModel):
    """低碳优化排产请求"""
    electricity_price: ElectricityPrice = Field(default_factory=ElectricityPrice)
    idle_energy_kw: float = Field(10.0, ge=0, description="设备空转能耗 kW")
    orders: list[ScheduleOrder] = Field(..., description="待排产订单")
    work_start_hour: float = Field(8.0, description="工作日开始时刻（0-24）")
    work_end_hour: float = Field(18.0, description="工作日结束时刻（0-24）")


class ScheduledTask(BaseModel):
    """排产任务"""
    order_id: str
    start_hour: float
    end_hour: float
    time_slot_type: str = Field(..., description="peak/valley/flat")
    energy_kwh: float = 0.0
    energy_cost: float = 0.0


class LowCarbonScheduleResult(BaseModel):
    """绿色排产计划结果"""
    schedule: list[ScheduledTask] = Field(..., description="绿色排产计划")
    total_energy_kwh: float = Field(..., description="总能耗 kWh")
    total_energy_cost: float = Field(..., description="总电费 元")
    traditional_energy_kwh: float = Field(..., description="传统方案能耗 kWh")
    traditional_energy_cost: float = Field(..., description="传统方案电费 元")
    savings_pct: float = Field(..., description="预计节省电能/电费百分比")
    algorithm: str = Field("genetic", description="genetic 或 reinforcement")


# ============== 产品碳足迹 (PCF) ==============

class PartCarbonData(BaseModel):
    """零件碳足迹数据（与 iDME 数字主线关联）"""
    part_id: Optional[str] = Field(None, description="零件ID，iDME Part/Material01 关联")
    part_name: str = Field("", description="零件名称")
    material_source: str = Field("", description="原材料来源/类型")
    material_weight_kg: float = Field(0.0, ge=0, description="材料重量 kg")
    material_emission_factor: float = Field(2.0, ge=0, description="材料碳排放系数 kg CO2/kg")
    transport_distance_km: float = Field(0.0, ge=0, description="运输距离 km")
    transport_emission_factor: float = Field(0.00012, ge=0, description="运输排放系数 kg CO2/km·kg")
    processing_hours: float = Field(0.0, ge=0, description="加工时长 h")
    processing_power_kw: float = Field(0.0, ge=0, description="加工功率 kW")
    grid_carbon_factor: float = Field(0.55, ge=0, description="电网碳因子 kg CO2/kWh")


class ProcessCarbonData(BaseModel):
    """工序碳足迹数据（与 iDME WorkingProcedure 关联）"""
    process_id: Optional[str] = Field(None, description="工序ID，iDME 关联")
    process_name: str = Field("", description="工序名称")
    duration_hours: float = Field(0.0, ge=0)
    power_kw: float = Field(0.0, ge=0)
    grid_carbon_factor: float = Field(0.55, ge=0)


class ProductPCFRequest(BaseModel):
    """产品碳足迹核算请求（产品完成生产时触发）"""
    product_id: Optional[str] = Field(None, description="产品ID，iDME 关联")
    product_name: str = Field("", description="产品名称")
    batch_id: Optional[str] = Field(None, description="生产批次ID")
    quantity: int = Field(1, gt=0, description="产量")
    parts: list[PartCarbonData] = Field(default_factory=list, description="零件碳数据")
    processes: list[ProcessCarbonData] = Field(default_factory=list, description="工序碳数据")


class CarbonLabelItem(BaseModel):
    """数字碳标签单项"""
    category: str = Field(..., description="material/transport/processing")
    item_name: str = Field("", description="零件或工序名称")
    co2_kg: float = Field(0.0, ge=0)
    share_pct: float = Field(0.0, ge=0, le=100)


class DigitalCarbonLabel(BaseModel):
    """数字碳标签"""
    product_id: Optional[str] = None
    product_name: str = ""
    total_co2_kg: float = 0.0
    co2_per_unit_kg: float = 0.0
    quantity: int = 1
    issued_at: Optional[str] = None
    items: list[CarbonLabelItem] = Field(default_factory=list)
    idme_trace: dict = Field(default_factory=dict, description="iDME 数字主线追溯")


class ProductPCFResult(BaseModel):
    """产品碳足迹核算结果"""
    total_co2_kg: float = Field(..., description="总 CO2 kg")
    co2_per_unit_kg: float = Field(..., description="单件 CO2 kg")
    carbon_label: DigitalCarbonLabel = Field(..., description="数字碳标签")
    breakdown: dict = Field(default_factory=dict, description="明细")
    high_emission_points: list[dict] = Field(default_factory=list, description="高碳排放点")
    ai_suggestions: str = Field("", description="AI 改进建议")


# ============== 智能质检 × 环保指标 ==============

class DefectWasteRequest(BaseModel):
    """次品造成的物料与能耗浪费核算（视觉检测出次品时触发）"""
    defect_count: int = Field(1, ge=0, description="次品数量")
    defect_types: list[str] = Field(default_factory=list, description="缺陷类型，如 漏焊/短路/断路")
    product_type: str = Field("PCB", description="产品类型")
    material_per_unit_g: dict = Field(
        default_factory=lambda: {"copper": 50, "tin": 5, "fr4": 80},
        description="单件物料用量(g)，如 {copper: 50, tin: 5}"
    )
    energy_per_unit_kwh: float = Field(0.15, ge=0, description="单件生产能耗 kWh")
    image_id: Optional[str] = Field(None, description="检测图像ID")


class DefectWasteResult(BaseModel):
    """次品浪费核算结果"""
    defect_count: int = 0
    material_waste_g: dict = Field(default_factory=dict, description="物料浪费 g，如 {copper: 50}")
    total_material_waste_g: float = 0.0
    energy_waste_kwh: float = 0.0
    co2_waste_kg: float = 0.0
    summary: str = Field("", description="浪费折算摘要")


class AnomalyDetectionRequest(BaseModel):
    """异常检测请求（设备故障苗头预警）"""
    device_id: str = Field("", description="设备ID")
    metrics: list[dict] = Field(..., description="时序指标，如 [{t:0, power:50, defect_rate:0}, {t:1, power:52, ...}]")
    metric_keys: list[str] = Field(default_factory=lambda: ["power", "defect_rate"], description="用于检测的指标键")


class AnomalyDetectionResult(BaseModel):
    """异常检测结果"""
    anomaly_score: float = Field(..., description="异常分数 0~1，越高越异常")
    recommend_shutdown: bool = Field(False, description="是否建议提前停机")
    anomaly_indices: list[int] = Field(default_factory=list, description="异常点索引")
    message: str = Field("", description="预警说明")


# ============== 绿色供应链评分 ==============

class SupplierInput(BaseModel):
    """供应商输入（用于绿色供应链评分与采购建议）"""
    supplier_id: Optional[str] = Field(None, description="供应商ID")
    supplier_name: str = Field(..., description="供应商名称")
    unit_price: float = Field(..., ge=0, description="单价（元/件）")
    carbon_intensity: float = Field(
        0.0, ge=0,
        description="碳强度 kg CO2/件，单件产品全生命周期碳排放。不填则 AI 根据报告推断"
    )
    quantity: int = Field(1, gt=0, description="采购数量")
    material_name: Optional[str] = Field(None, description="物料/产品名称")
    # 可选：供 LLM 分析的环保报告摘要、认证信息（用户粘贴或系统抓取后传入）
    environmental_report_text: Optional[str] = Field(None, description="环保报告摘要/原文，AI 分析用")
    iso14001_certified: Optional[bool] = Field(None, description="是否通过 ISO 14001 认证")
    report_source_url: Optional[str] = Field(None, description="报告来源 URL（追溯用）")


class GreenSupplyChainScoreRequest(BaseModel):
    """绿色供应链评分请求：AI 分析供应商环保报告、ISO 14001 认证"""
    suppliers: list[SupplierInput] = Field(..., description="待评分供应商列表")
    product_name: Optional[str] = Field(None, description="采购产品名称")
    priority: str = Field("balanced", description="balanced=价格与碳平衡, cost_first=价格优先, green_first=绿色优先")


class SupplierGreenScore(BaseModel):
    """单个供应商绿色评分"""
    supplier_id: Optional[str] = None
    supplier_name: str = ""
    green_score: float = Field(..., ge=0, le=100, description="绿色评分 0~100")
    unit_price: float = 0.0
    carbon_intensity: float = 0.0
    iso14001_status: str = Field("", description="已认证/未认证/待核实")
    report_summary: str = Field("", description="AI 对环保报告的分析摘要")
    strengths: list[str] = Field(default_factory=list, description="环保优势")
    risks: list[str] = Field(default_factory=list, description="环保风险")


class GreenSupplyChainScoreResult(BaseModel):
    """绿色供应链评分结果"""
    success: bool = True
    suppliers: list[SupplierGreenScore] = Field(..., description="各供应商绿色评分")
    ai_analysis: str = Field("", description="AI 综合分析与建议")
    warnings: list[str] = Field(default_factory=list, description="黄色预警：便宜但排碳高的供应商")


class ProcurementRecommendationRequest(BaseModel):
    """采购建议请求：综合考虑价格与碳强度"""
    suppliers: list[SupplierInput] = Field(..., description="候选供应商")
    product_name: Optional[str] = Field(None)
    quantity: int = Field(1, gt=0)
    green_weight: float = Field(0.4, ge=0, le=1, description="绿色权重 0~1，0=只看价格，1=只看碳")


class ProcurementRecommendationResult(BaseModel):
    """采购建议结果"""
    success: bool = True
    recommended_supplier: str = Field(..., description="推荐供应商")
    recommended_reason: str = Field("", description="推荐理由")
    yellow_warnings: list[dict] = Field(
        default_factory=list,
        description="黄色预警：{supplier, price, carbon_intensity, message}"
    )
    greener_alternatives: list[dict] = Field(
        default_factory=list,
        description="更环保的替代方案 {supplier, price, carbon_intensity, green_score}"
    )
    ai_recommendation: str = Field("", description="AI 采购建议（综合考虑价格与碳强度）")


# ============== 最优生产方案（综合商业分析+绿色环保+智能检测） ==============

class PlanEquipmentItem(BaseModel):
    """生产方案-设备项（可导入设备管理）"""
    equipment_code: str = Field(..., description="设备编码")
    equipment_name: str = Field(..., description="设备名称")
    manufacturer: str = Field("", description="生产厂家")
    brand: str = Field("", description="品牌")
    model: str = Field("", description="规格型号")
    supplier: str = Field("", description="供应商")
    location: str = Field("", description="位置")
    technical_params: str = Field("", description="技术参数")


class PlanMaterialItem(BaseModel):
    """生产方案-物料项（可导入物料管理）"""
    material_code: str = Field(..., description="物料编号")
    material_name: str = Field(..., description="物料名称")
    model: str = Field("", description="规格型号")
    quantity: float = Field(1.0, ge=0, description="用量")
    unit: str = Field("件", description="单位")
    supplier: str = Field("", description="供应商")


class PlanProcedureItem(BaseModel):
    """生产方案-工序项（可导入工序管理）"""
    procedure_code: str = Field(..., description="工序编号")
    procedure_name: str = Field(..., description="工序名称")
    production_steps: str = Field("", description="生产步骤")
    production_inspection_equipment: str = Field("", description="检测设备")
    operator: str = Field("", description="操作人员")
    equipment_codes: list[str] = Field(default_factory=list, description="关联设备编码")
    material_codes: list[str] = Field(default_factory=list, description="关联物料编码")


class PlanProcessRouteItem(BaseModel):
    """生产方案-工艺路线项（可导入工艺管理）"""
    route_code: str = Field(..., description="工艺编号")
    route_name: str = Field(..., description="工艺名称")
    product: str = Field("", description="所属产品")
    version: str = Field("V1.0", description="版本")
    description: str = Field("", description="工艺描述")
    procedure_sequence: list[str] = Field(default_factory=list, description="工序编码顺序")


class OptimalProductionPlanRequest(BaseModel):
    """最优生产方案请求：综合商业分析、绿色环保、智能检测"""
    product_name: str = Field(..., description="产品名称")
    quantity: int = Field(100, gt=0, description="计划产量")
    # 商业分析输入（可选，有则参与综合评估）
    cost_profit: Optional[CostProfitData] = Field(None)
    market_data: Optional[MarketData] = Field(None)
    # 绿色环保输入（可选）
    carbon_footprint: Optional[dict] = Field(None, description="碳足迹数据")
    green_supply_chain: Optional[list[dict]] = Field(None, description="绿色供应链评分")
    # 智能检测输入（可选）
    defect_config: Optional[dict] = Field(None, description="次品浪费折算配置")
    anomaly_config: Optional[dict] = Field(None, description="异常检测配置")
    # 设备、物料、工序、工艺（可选，有则作为基础，无则生成示例）
    equipment: list[PlanEquipmentItem] = Field(default_factory=list)
    materials: list[PlanMaterialItem] = Field(default_factory=list)
    procedures: list[PlanProcedureItem] = Field(default_factory=list)
    process_routes: list[PlanProcessRouteItem] = Field(default_factory=list)


class OptimalProductionPlanResult(BaseModel):
    """最优生产方案结果：可一键导入设备/物料/工序/工艺，导出 Excel/PDF"""
    success: bool = True
    product_name: str = ""
    quantity: int = 0
    # 综合评估摘要
    summary: str = Field("", description="AI 综合评估摘要")
    # 商业分析
    business_analysis: dict = Field(default_factory=dict, description="利润、敏感度、市场策略")
    # 绿色环保
    green_metrics: dict = Field(default_factory=dict, description="碳足迹、PUE、供应链评分")
    # 智能检测
    inspection_config: dict = Field(default_factory=dict, description="质检配置、浪费折算、异常检测")
    # 可导入数据
    equipment: list[PlanEquipmentItem] = Field(default_factory=list)
    materials: list[PlanMaterialItem] = Field(default_factory=list)
    procedures: list[PlanProcedureItem] = Field(default_factory=list)
    process_routes: list[PlanProcessRouteItem] = Field(default_factory=list)
    # 流程图 Mermaid
    flowchart_mermaid: str = Field("", description="Mermaid 流程图代码")
