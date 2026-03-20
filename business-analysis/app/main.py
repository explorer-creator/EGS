# -*- coding: utf-8 -*-
"""
商业决策智能分析 - FastAPI 主入口
RESTful API，返回 JSON 格式分析结果（预测数值 + AI 文本建议）
"""
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_cors_origins
from app.models.schemas import (
    OptimalProductionPlanRequest,
    OptimalProductionPlanResult,
    DefectWasteRequest,
    DefectWasteResult,
    AnomalyDetectionRequest,
    AnomalyDetectionResult,
    GreenSupplyChainScoreRequest,
    GreenSupplyChainScoreResult,
    ProcurementRecommendationRequest,
    ProcurementRecommendationResult,
    CostProfitData,
    EnergyMonitoringRequest,
    LowCarbonScheduleRequest,
    ProductPCFRequest,
    CarbonFootprintRequest,
    EnergyOptimizationRequest,
    HistoricalBidRecord,
    BidFeedbackRecord,
    MaterialPriceRecord,
    PricePredictionRequest,
    ProfitOptimizationRequest,
    QuotationDataForPdf,
    NegotiationStrategyRequest,
    MultiPlanComparisonRequest,
    ProfitSensitivityRequest,
    ProfitSensitivityResponse,
    MarketStrategyRequest,
    MarketStrategyResponse,
    BusinessAnalysisInput,
    NaturalReportRequest,
    FullAnalysisResponse,
    SensitivityScenario,
    QuotationInput,
    FloorPriceResult,
    QuotationFullInput,
    QuotationFullResult,
)
from app.services import (
    run_profit_sensitivity,
    compute_low_carbon_schedule,
    compute_product_pcf_with_ai,
    calculate_pue,
    calculate_carbon_footprint,
    generate_energy_optimization_report,
    get_dashboard_data,
    record_green_metrics,
    generate_quotation_pdf,
    generate_negotiation_strategy,
    generate_multi_plan_comparison,
    fetch_commodity_prices,
    get_commodity_price_summary,
    compute_correction_factor,
    apply_correction,
    get_correction_summary,
    generate_market_strategy,
    generate_natural_language_report,
    generate_customer_email_wording,
    generate_product_context_demo,
    calculate_floor_price,
    calculate_quotation_full,
    predict_success_rate,
    retrain_model,
    predict_material_price_impact,
    optimize_deal_price,
    save_quotation_to_idme,
    get_idme_health,
    compute_defect_waste,
    run_anomaly_detection,
    score_suppliers_green,
    get_procurement_recommendation,
    get_procurement_recommendation_with_ai,
    generate_optimal_production_plan,
    export_production_plan_to_excel,
    export_production_plan_to_pdf,
)
from app.services.quotation_service import _suggested_price_from_market
from app.config import get_llm_api_key, get_llm_base_url, get_llm_model


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield
    # 可在此做清理


app = FastAPI(
    title="商业决策智能分析",
    description="工业软件 - 利润敏感度分析、市场进入策略建议，预留 iDME 对接",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=get_cors_origins(),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ============== 利润敏感度分析 ==============

@app.post("/api/business/sensitivity", response_model=ProfitSensitivityResponse)
async def profit_sensitivity(req: ProfitSensitivityRequest) -> ProfitSensitivityResponse:
    """
    利润敏感度分析：当原材料/人工/能耗变动时，预测对总利润的影响。
    使用 Scikit-learn Linear Regression。
    """
    return run_profit_sensitivity(req.cost_profit, req.scenarios)


# ============== 市场进入策略（LLM） ==============

@app.post("/api/business/market-strategy", response_model=MarketStrategyResponse)
async def market_strategy(req: MarketStrategyRequest) -> MarketStrategyResponse:
    """
    根据市场数据和用户画像，调用 LLM 生成市场进入策略建议。
    支持智谱、GPT-4、千问等 OpenAI 兼容 API。
    """
    api_key = get_llm_api_key()
    if not api_key:
        raise HTTPException(
            status_code=500,
            detail="请配置 LLM_API_KEY、OPENAI_API_KEY 或 DASHSCOPE_API_KEY",
        )
    return await generate_market_strategy(
        req.product_name,
        req.cost_profit,
        req.market_data,
        api_key=api_key,
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )


# ============== 完整分析（敏感度 + 市场策略） ==============

@app.post("/api/business/full-analysis", response_model=FullAnalysisResponse)
async def full_analysis(input_data: BusinessAnalysisInput) -> FullAnalysisResponse:
    """
    完整商业分析：利润敏感度预测 + AI 市场进入策略建议。
    返回 JSON，包含预测数值和 AI 生成的文本建议。
    """
    scenarios = [
        SensitivityScenario(factor="bom_cost", change_percent=5),
        SensitivityScenario(factor="labor_cost", change_percent=5),
        SensitivityScenario(factor="energy_cost", change_percent=10),
        SensitivityScenario(factor="power_fluctuation", change_percent=10),
        SensitivityScenario(factor="logistics_delay", change_percent=20),
    ]
    sensitivity = run_profit_sensitivity(input_data.cost_profit, scenarios)

    api_key = get_llm_api_key()
    if api_key:
        strategy = await generate_market_strategy(
            input_data.product_name,
            input_data.cost_profit,
            input_data.market_data,
            api_key=api_key,
            base_url=get_llm_base_url(),
            model=get_llm_model(),
        )
        # 自然语言报告：将图表/数据转化为可读文字
        sens_dicts = [r.model_dump() for r in sensitivity.results]
        natural_report = await generate_natural_language_report(
            input_data.product_name,
            input_data.cost_profit,
            input_data.market_data,
            sens_dicts,
            api_key=api_key,
            base_url=get_llm_base_url(),
            model=get_llm_model(),
        )
    else:
        strategy = MarketStrategyResponse(
            success=False,
            suggestion="请配置 LLM API Key 以获取市场策略建议",
            product_name=input_data.product_name,
        )
        natural_report = None

    return FullAnalysisResponse(
        product_name=input_data.product_name,
        sensitivity=sensitivity,
        market_strategy=strategy,
        natural_report=natural_report,
    )


# ============== 智能报价 ==============

@app.post("/api/quotation/floor-price", response_model=FloorPriceResult)
async def quotation_floor_price(req: QuotationInput) -> FloorPriceResult:
    """
    计算不亏本的底线价格。
    输入：BOM 实时价格、加工工时、设备折旧、能源消耗、物流成本。
    支持 iDME 元模型关联（batch_id、bom_id）。
    """
    return calculate_floor_price(req)


@app.post("/api/quotation/full", response_model=QuotationFullResult)
async def quotation_full(req: QuotationFullInput) -> QuotationFullResult:
    """
    智能报价完整流程（逻辑闭环）：
    1. 计算底价、建议价、预计利润率
    2. LinearRegression 预测成功率（基于历史中标数据）
    3. LLM 生成给客户的邮件话术
    4. 实时写入 iDME 风格数据库
    5. 返回 JSON（含 ui_suggestions：前端 ECharts 展示建议）
    """
    total_cost = req.material_cost + req.labor_cost + req.energy_cost
    floor_price = total_cost / req.quantity if req.quantity > 0 else 0.0
    suggested_price = _suggested_price_from_market(
        floor_price, req.competitor_price, req.customer_level
    )
    revenue = suggested_price * req.quantity
    profit = revenue - total_cost
    profit_margin = (profit / revenue) if revenue > 0 else 0.0

    success_rate = predict_success_rate(
        suggested_price, floor_price,
        req.competitor_price, req.customer_level,
    )

    api_key = get_llm_api_key()
    email_wording = None
    if api_key:
        email_wording = await generate_customer_email_wording(
            req.product_name or "产品",
            suggested_price, floor_price, profit_margin,
            req.customer_level, req.competitor_price,
            api_key=api_key,
            base_url=get_llm_base_url(),
            model=get_llm_model(),
        )

    idme_id = await save_quotation_to_idme(
        floor_price, suggested_price, profit_margin, success_rate,
        req.product_name, req.batch_id, req.bom_id, req.quantity,
    )

    # AI 子模块：价格预测 & 利润优化
    material_impact = None
    deal_price = None
    total_cost = req.material_cost + req.labor_cost + req.energy_cost
    material_share = (req.material_cost / total_cost) if total_cost > 0 else 0.5

    if req.historical_material_prices and len(req.historical_material_prices) >= 3:
        records = [MaterialPriceRecord(date_index=i, unit_price=p) for i, p in enumerate(req.historical_material_prices, 1)]
        share = req.material_cost_share if req.material_cost_share is not None else material_share
        pred = predict_material_price_impact(records, share, floor_price)
        material_impact = pred.model_dump()

    if req.capacity_utilization is not None:
        opt = optimize_deal_price(suggested_price, req.capacity_utilization, floor_price)
        deal_price = opt.suggested_deal_price

    # 大宗商品实时市价（铜、铝）
    commodity_prices = await fetch_commodity_prices()

    # 误差补偿：AI 自动学习历史报价与实际结标价偏差，自我修正
    correction_applied = None
    if req.feedback_records and len(req.feedback_records) >= 2:
        factor = compute_correction_factor(req.feedback_records)
        corrected = apply_correction(suggested_price, factor)
        correction_applied = {
            "correction_factor": factor,
            "original_suggested": suggested_price,
            "corrected_suggested": corrected,
            "message": get_correction_summary(req.feedback_records)["message"],
        }

    # 绿色供应链：供应商列表时，综合考虑价格与碳强度，便宜但排碳高则黄色预警
    green_supply_chain_advice = None
    if req.suppliers and len(req.suppliers) >= 1:
        proc_req = ProcurementRecommendationRequest(
            suppliers=req.suppliers,
            product_name=req.product_name,
            quantity=req.quantity,
            green_weight=0.4,
        )
        proc_result = await get_procurement_recommendation_with_ai(
            proc_req,
            api_key=get_llm_api_key(),
            base_url=get_llm_base_url(),
            model=get_llm_model(),
        )
        green_supply_chain_advice = {
            "recommended_supplier": proc_result.recommended_supplier,
            "recommended_reason": proc_result.recommended_reason,
            "yellow_warnings": proc_result.yellow_warnings,
            "greener_alternatives": proc_result.greener_alternatives,
            "ai_recommendation": proc_result.ai_recommendation,
        }

    return calculate_quotation_full(
        req, success_rate, email_wording, idme_id,
        suggested_deal_price=deal_price,
        material_price_impact=material_impact,
        commodity_prices=commodity_prices,
        correction_applied=correction_applied,
        green_supply_chain_advice=green_supply_chain_advice,
    )


@app.post("/api/quotation/success-rate")
async def quotation_success_rate(
    quoted_price: float,
    floor_price: float,
    competitor_price: float | None = None,
    customer_level: str = "B",
) -> dict:
    """基于历史中标数据的成功率预测（LinearRegression）"""
    rate = predict_success_rate(quoted_price, floor_price, competitor_price, customer_level)
    return {"success_rate": round(rate, 4)}


@app.post("/api/quotation/train-success-model")
async def train_success_model(records: list[HistoricalBidRecord]) -> dict:
    """使用自定义历史中标数据重新训练成功率预测模型"""
    retrain_model(records)
    return {"success": True, "message": f"已使用 {len(records)} 条数据训练"}


@app.post("/api/quotation/price-prediction")
async def quotation_price_prediction(req: PricePredictionRequest) -> dict:
    """
    价格预测模型：调用历史数据，回归算法预测未来一个月原材料价格波动对当前报价的影响
    """
    result = predict_material_price_impact(
        req.historical_prices,
        req.material_cost_share,
        req.current_floor_price,
    )
    return result.model_dump()


@app.post("/api/quotation/generate-pdf")
async def quotation_generate_pdf(req: QuotationDataForPdf):
    """
    报价单自动生成：根据计算出的数据，自动撰写专业 PDF 报价建议书，包含价格明细说明。
    """
    api_key = get_llm_api_key()
    pdf_bytes, filename = await generate_quotation_pdf(
        req,
        api_key=api_key,
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )


@app.post("/api/quotation/negotiation-strategy")
async def quotation_negotiation_strategy(req: NegotiationStrategyRequest) -> dict:
    """
    谈判策略助手：输入客户背景（如：长期合作伙伴、价格敏感型、急单），
    AI 给出针对性谈判话术和最大折扣力度建议。
    """
    result = await generate_negotiation_strategy(
        req,
        api_key=get_llm_api_key(),
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )
    return result.model_dump()


@app.post("/api/quotation/multi-plan-comparison")
async def quotation_multi_plan_comparison(req: MultiPlanComparisonRequest) -> dict:
    """
    多方案对比：AI 自动生成经济型、标准型、高端型三个报价方案，
    并分析各方案的利润率和中标概率。
    """
    result = await generate_multi_plan_comparison(
        req,
        api_key=get_llm_api_key(),
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )
    return result.model_dump()


@app.post("/api/quotation/profit-optimization")
async def quotation_profit_optimization(req: ProfitOptimizationRequest) -> dict:
    """
    利润优化算法：根据产能利用率推荐建议成交价。
    产能闲置→降价抢单，产能饱和→提高溢价。
    """
    result = optimize_deal_price(
        req.base_price,
        req.capacity_utilization,
        req.floor_price,
    )
    return result.model_dump()


# ============== 自然语言报告 ==============

@app.post("/api/business/natural-report")
async def natural_report(req: NaturalReportRequest) -> dict:
    """
    将枯燥的图表/数据转化为自然语言报告。
    示例：「AI 分析显示：当前原材料价格处于高位，建议针对华南地区的小型电子加工厂
    （核心客群）推出租赁方案以保证利润率。」
    """
    api_key = get_llm_api_key()
    if not api_key:
        raise HTTPException(
            status_code=500,
            detail="请配置 LLM_API_KEY、OPENAI_API_KEY 或 DASHSCOPE_API_KEY",
        )
    sens = req.sensitivity_results or []
    report = await generate_natural_language_report(
        req.product_name,
        req.cost_profit,
        req.market_data,
        sens,
        api_key=api_key,
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )
    return {"success": True, "product_name": req.product_name, "report": report}


# ============== 产品绿色/商业分析演示（LLM，不依赖 8003） ==============

@app.post("/api/product-context-demo")
async def product_context_demo(body: dict):
    """
    根据产品名称生成绿色生产、商业分析的产品专属演示内容。
    用于从产品问答跳转后展示，纯 LLM 调用，绕过 8003。
    """
    product = (body.get("product") or "").strip() if body else ""
    if not product:
        return {"success": False, "message": "请输入产品名称"}
    return await generate_product_context_demo(
        product,
        api_key=get_llm_api_key(),
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )


# ============== 产品综合报告（聚合多接口，返回可视化数据） ==============

def _default_cost_profit() -> CostProfitData:
    """默认成本利润数据（演示用）"""
    return CostProfitData(
        bom_cost=50000,
        labor_cost=20000,
        energy_cost=8000,
        selling_price=120,
        quantity=1000,
        overhead_rate=0.15,
        power_fluctuation=0.05,
        logistics_delay_cost=2000,
    )


@app.post("/api/product-report")
async def product_report(body: dict):
    """
    产品综合报告：聚合敏感度分析、绿色仪表盘、LLM 文本，返回可视化报告数据。
    调用 run_profit_sensitivity、get_dashboard_data、generate_product_context_demo。
    """
    from app.services.green_dashboard_service import get_dashboard_data

    product = (body.get("product") or "").strip() if body else ""
    if not product:
        return {"success": False, "message": "请输入产品名称"}

    result = {"success": True, "product": product}

    # 1. 利润敏感度分析
    try:
        cp = _default_cost_profit()
        scenarios = [
            SensitivityScenario(factor="bom_cost", change_percent=5),
            SensitivityScenario(factor="labor_cost", change_percent=5),
            SensitivityScenario(factor="energy_cost", change_percent=10),
            SensitivityScenario(factor="power_fluctuation", change_percent=10),
            SensitivityScenario(factor="logistics_delay", change_percent=20),
        ]
        sens = run_profit_sensitivity(cp, scenarios)
        result["sensitivity"] = sens.model_dump()
    except Exception as e:
        result["sensitivity"] = {"error": str(e), "results": []}

    # 2. 绿色仪表盘数据
    try:
        result["green_dashboard"] = get_dashboard_data(30)
    except Exception as e:
        result["green_dashboard"] = {"error": str(e), "dates": [], "carbon_trend": [], "pue_trend": []}

    # 3. LLM 绿色/商业分析文本
    try:
        ctx = await generate_product_context_demo(
            product,
            api_key=get_llm_api_key(),
            base_url=get_llm_base_url(),
            model=get_llm_model(),
        )
        result["green_summary"] = ctx.get("green_summary", "")
        result["business_summary"] = ctx.get("business_summary", "")
    except Exception as e:
        result["green_summary"] = f"针对「{product}」的绿色生产：建议关注能效监测、碳足迹核算、低碳排产与绿色供应链。"
        result["business_summary"] = f"针对「{product}」的商业分析：建议关注利润敏感度、智能报价与市场进入策略。"

    return result


# ============== iDME 对接预留 ==============

@app.get("/api/business/idme/health")
async def idme_health():
    """iDME 数据底座对接状态（预留）"""
    return get_idme_health()


# ============== 大宗商品实时价格 ==============

@app.get("/api/quotation/commodity-prices")
async def quotation_commodity_prices():
    """
    通过 API 实时抓取大宗商品（如铜、铝）的国际市价。
    用于报价成本核算，体现商业实用性。
    """
    return await fetch_commodity_prices()


@app.get("/api/quotation/commodity-prices/summary")
async def commodity_prices_summary():
    """大宗商品价格服务说明（评委展示用）"""
    return get_commodity_price_summary()


# ============== 误差补偿与自我修正 ==============

@app.post("/api/quotation/feedback-correction")
async def quotation_feedback_correction(records: list[BidFeedbackRecord]) -> dict:
    """
    AI 自动学习历史报价与实际结标价的偏差，并进行自我修正。
    体现强化学习/反馈闭环思想。
    """
    summary = get_correction_summary(records)
    if records:
        summary["correction_factor"] = compute_correction_factor(records)
    return summary


@app.post("/api/quotation/apply-correction")
async def quotation_apply_correction(
    suggested_price: float,
    records: list[BidFeedbackRecord],
) -> dict:
    """根据历史结标偏差，对建议价应用自我修正"""
    factor = compute_correction_factor(records)
    corrected = apply_correction(suggested_price, factor)
    return {
        "original_price": suggested_price,
        "correction_factor": factor,
        "corrected_price": corrected,
    }


# ============== 最优生产方案（综合商业分析+绿色环保+智能检测） ==============

@app.post("/api/production-plan/optimal", response_model=OptimalProductionPlanResult)
async def production_plan_optimal(req: OptimalProductionPlanRequest) -> OptimalProductionPlanResult:
    """
    综合商业分析、绿色环保、智能检测，生成最优生产方案。
    可一键导入到设备管理、物料管理、工序管理、工艺管理，生成流程图。
    """
    return await generate_optimal_production_plan(
        req,
        api_key=get_llm_api_key(),
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )


@app.post("/api/production-plan/export-excel")
async def production_plan_export_excel(req: OptimalProductionPlanRequest):
    """
    生成最优生产方案并导出 Excel。
    含：导入说明、设备、物料、工序、工艺路线、商业分析、绿色环保、智能检测、流程图。
    各 Sheet 表头与前端批量导入格式一致，可一键导入到对应管理模块。
    """
    plan = await generate_optimal_production_plan(
        req,
        api_key=get_llm_api_key(),
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )
    excel_bytes = export_production_plan_to_excel(plan)
    filename = f"产品生产方案_{plan.product_name}_{__import__('datetime').datetime.now().strftime('%Y%m%d%H%M')}.xlsx"
    return Response(
        content=excel_bytes,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )


@app.post("/api/production-plan/export-pdf")
async def production_plan_export_pdf(req: OptimalProductionPlanRequest):
    """
    生成最优生产方案并导出 PDF。
    含：综合摘要、商业分析、绿色环保、智能检测、设备/物料/工序/工艺清单、流程图。
    """
    plan = await generate_optimal_production_plan(
        req,
        api_key=get_llm_api_key(),
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )
    pdf_bytes = export_production_plan_to_pdf(plan)
    from datetime import datetime
    filename = f"产品生产方案_{plan.product_name}_{datetime.now().strftime('%Y%m%d%H%M')}.pdf"
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )


@app.post("/api/production-plan/export-all")
async def production_plan_export_all(req: OptimalProductionPlanRequest):
    """
    一键导出 Excel + PDF。
    返回 JSON，含 plan 摘要及 excel_url、pdf_url（需前端分别请求下载，或返回 base64）。
    为简化，此处返回 plan + 提示：请分别调用 export-excel 和 export-pdf 获取文件。
    """
    plan = await generate_optimal_production_plan(
        req,
        api_key=get_llm_api_key(),
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )
    return {
        "success": True,
        "plan": plan.model_dump(),
        "message": "请分别调用 POST /api/production-plan/export-excel 和 /api/production-plan/export-pdf 获取 Excel 和 PDF 文件。或使用同一请求体连续调用两次。",
    }


# ============== 绿色生产 ==============

@app.post("/api/green/energy-monitoring")
async def green_energy_monitoring(req: EnergyMonitoringRequest):
    """
    能效监测接口：接收设备实时功率数据，计算 PUE（能源效率）指标。
    PUE = 总能耗 / 生产设备能耗，越接近 1 越优
    """
    result = calculate_pue(req)
    record_green_metrics(result.pue, 0)  # CO2 由碳足迹接口单独记录
    return result.model_dump()


@app.post("/api/green/carbon-footprint")
async def green_carbon_footprint(req: CarbonFootprintRequest):
    """
    碳足迹算法：根据生产工时和材料重量，自动换算成 CO2 排放量。
    """
    result = calculate_carbon_footprint(req)
    record_green_metrics(1.5, result.total_co2_kg)  # 记录到仪表盘
    return result.model_dump()


@app.post("/api/green/energy-optimization")
async def green_energy_optimization(req: EnergyOptimizationRequest):
    """
    AI 优化建议：根据当前能耗分布，调用大模型生成「节能降碳改进报告」。
    """
    report = await generate_energy_optimization_report(
        req,
        api_key=get_llm_api_key(),
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )
    return {"report": report, "success": True}


@app.post("/api/green/product-carbon-footprint")
async def green_product_carbon_footprint(req: ProductPCFRequest):
    """
    产品碳足迹 (PCF) 核算：产品完成生产时自动计算并生成数字碳标签。
    数据关联：零件原材料来源、运输距离、加工时长与碳排放系数绑定（iDME 数字主线）。
    AI 自动识别高碳排放点并给出改进建议。
    """
    result = await compute_product_pcf_with_ai(
        req,
        api_key=get_llm_api_key(),
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )
    return result.model_dump()


@app.post("/api/green/low-carbon-schedule")
async def green_low_carbon_schedule(req: LowCarbonScheduleRequest):
    """
    低碳优化算法：遗传算法在保证交付时间的前提下，
    自动调整设备启停顺序，最大化利用绿电（谷时）或降低总能耗。
    输出绿色排产计划，对比传统方案预计节省的电能百分比。
    """
    result = compute_low_carbon_schedule(req)
    return result.model_dump()


@app.post("/api/green/supply-chain-score", response_model=GreenSupplyChainScoreResult)
async def green_supply_chain_score(req: GreenSupplyChainScoreRequest) -> GreenSupplyChainScoreResult:
    """
    绿色供应链评分：AI 分析供应商环保报告、ISO 14001 认证情况。
    若提供 environmental_report_text，LLM 深度分析；否则按碳强度与认证规则推断。
    """
    return await score_suppliers_green(
        req,
        api_key=get_llm_api_key(),
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )


@app.post("/api/green/procurement-recommendation", response_model=ProcurementRecommendationResult)
async def green_procurement_recommendation(req: ProcurementRecommendationRequest) -> ProcurementRecommendationResult:
    """
    采购建议：综合考虑价格与碳强度。
    若某供应商便宜但排碳极高，给出黄色预警并推荐更环保的替代方案。
    """
    return await get_procurement_recommendation_with_ai(
        req,
        api_key=get_llm_api_key(),
        base_url=get_llm_base_url(),
        model=get_llm_model(),
    )


@app.post("/api/green/defect-waste", response_model=DefectWasteResult)
async def green_defect_waste(req: DefectWasteRequest) -> DefectWasteResult:
    """
    智能质检 × 环保指标：视觉模型检测出次品时，自动折算物料浪费与能耗浪费。
    如：浪费了多少克铜、多少度电、等效 CO2。
    """
    return compute_defect_waste(req)


@app.post("/api/green/anomaly-detection", response_model=AnomalyDetectionResult)
async def green_anomaly_detection(req: AnomalyDetectionRequest) -> AnomalyDetectionResult:
    """
    异常检测算法：在设备出现故障苗头时提前预警，建议停机，防止大批量废品。
    实现零缺陷、零浪费的绿色生产目标。
    """
    return run_anomaly_detection(req)


@app.get("/api/green/dashboard")
async def green_dashboard(days: int = 30):
    """
    绿色生产仪表盘数据接口。
    供 Dashboard 展示：减碳趋势图、PUE 趋势等。
    """
    return get_dashboard_data(days)


# ============== 健康检查 ==============

@app.get("/health")
async def health():
    return {"status": "ok", "service": "business-decision-analysis"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8001,
        reload=True,
    )
