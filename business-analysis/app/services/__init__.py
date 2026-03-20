# -*- coding: utf-8 -*-
from .sensitivity_analysis import run_profit_sensitivity
from .llm_service import (
    generate_market_strategy,
    generate_natural_language_report,
    generate_customer_email_wording,
    generate_product_context_demo,
)
from .quotation_service import calculate_floor_price, calculate_quotation_full
from .success_rate_service import predict_success_rate, retrain_model
from .price_prediction_service import predict_material_price_impact
from .profit_optimization_service import optimize_deal_price
from .commodity_price_service import fetch_commodity_prices, get_commodity_price_summary
from .feedback_correction_service import (
    compute_correction_factor,
    apply_correction,
    get_correction_summary,
)
from .low_carbon_schedule_service import compute_low_carbon_schedule
from .pcf_service import compute_product_pcf, compute_product_pcf_with_ai
from .green_production_service import (
    calculate_pue,
    calculate_carbon_footprint,
    generate_energy_optimization_report,
)
from .green_dashboard_service import get_dashboard_data, record_green_metrics
from .quotation_llm_service import (
    generate_quotation_pdf,
    generate_negotiation_strategy,
    generate_multi_plan_comparison,
)
from .inspection_green_service import compute_defect_waste, run_anomaly_detection
from .optimal_production_plan_service import (
    generate_optimal_production_plan,
    export_production_plan_to_excel,
    export_production_plan_to_pdf,
)
from .green_supply_chain_service import (
    score_suppliers_green,
    get_procurement_recommendation,
    get_procurement_recommendation_with_ai,
)
from .idme_client import (
    fetch_bom_from_idme,
    fetch_material_cost_from_idme,
    build_cost_profit_from_idme,
    build_quotation_from_idme,
    save_quotation_to_idme,
    get_idme_health,
)

__all__ = [
    "run_profit_sensitivity",
    "generate_market_strategy",
    "generate_natural_language_report",
    "generate_customer_email_wording",
    "generate_product_context_demo",
    "calculate_floor_price",
    "calculate_quotation_full",
    "predict_success_rate",
    "retrain_model",
    "predict_material_price_impact",
    "optimize_deal_price",
    "generate_quotation_pdf",
    "generate_negotiation_strategy",
    "generate_multi_plan_comparison",
    "fetch_commodity_prices",
    "get_commodity_price_summary",
    "compute_correction_factor",
    "apply_correction",
    "get_correction_summary",
    "calculate_pue",
    "calculate_carbon_footprint",
    "generate_energy_optimization_report",
    "get_dashboard_data",
    "record_green_metrics",
    "compute_low_carbon_schedule",
    "compute_product_pcf",
    "compute_product_pcf_with_ai",
    "compute_defect_waste",
    "run_anomaly_detection",
    "score_suppliers_green",
    "get_procurement_recommendation",
    "get_procurement_recommendation_with_ai",
    "generate_optimal_production_plan",
    "export_production_plan_to_excel",
    "export_production_plan_to_pdf",
    "fetch_bom_from_idme",
    "fetch_material_cost_from_idme",
    "build_cost_profit_from_idme",
    "build_quotation_from_idme",
    "save_quotation_to_idme",
    "get_idme_health",
]
