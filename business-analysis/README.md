# 商业决策智能分析模块

工业软件系统 - 基于 Python/FastAPI 的商业决策智能分析服务。

## 功能

1. **数据模型**：成本与利润（BOM、人工、能耗、售价、毛利率、**电力波动**、**物流延迟**）、市场与用户（目标行业、用户画像、市场渗透率、竞品价格）
2. **多因子利润敏感度分析**：Scikit-learn **Random Forest**，特征包含 BOM、人工、能耗、**电力波动**、**物流延迟**，不再仅用 利润=售价-成本
3. **市场进入策略**：调用 LLM（智谱/GPT-4/千问）生成市场进入策略建议
4. **自然语言报告**：将枯燥的图表/数据转化为可读文字，如「AI 分析显示：当前原材料价格处于高位，建议针对华南地区的小型电子加工厂（核心客群）推出租赁方案以保证利润率」
5. **智能报价**：`calculate_floor_price()` 计算不亏本底线价格，输入 BOM 实时价、工时、设备折旧、能源、物流
6. **iDME 对接**：预留与 iDME 数据底座对接接口，元模型关联：报价单 ↔ 生产批次(WorkingPlan) ↔ 物料清单(Material_BOM)

## 快速启动

```bash
cd business-analysis
pip install -r requirements.txt
# 配置 LLM API Key
export DASHSCOPE_API_KEY=sk-xxx   # 或 LLM_API_KEY / OPENAI_API_KEY
python run.py
```

服务默认运行在 `http://0.0.0.0:8001`。

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/business/sensitivity` | 利润敏感度分析（多因子 Random Forest） |
| POST | `/api/business/market-strategy` | 市场进入策略（LLM） |
| POST | `/api/business/natural-report` | 自然语言报告（图表→文字） |
| POST | `/api/business/full-analysis` | 完整分析（敏感度+策略+自然语言报告） |
| POST | `/api/quotation/floor-price` | 智能报价 - 底线价格 |
| POST | `/api/quotation/full` | 智能报价完整（底价+建议价+利润率+成功率+邮件话术+iDME 写入） |
| POST | `/api/quotation/success-rate` | 成功率预测（LinearRegression） |
| POST | `/api/quotation/train-success-model` | 使用历史中标数据训练成功率模型 |
| POST | `/api/quotation/price-prediction` | 价格预测：原材料波动对报价的影响（回归） |
| POST | `/api/quotation/profit-optimization` | 利润优化：按产能利用率推荐成交价 |
| POST | `/api/quotation/generate-pdf` | 报价单 PDF 自动生成 |
| POST | `/api/quotation/negotiation-strategy` | 谈判策略助手（话术+最大折扣） |
| POST | `/api/quotation/multi-plan-comparison` | 多方案对比（经济/标准/高端） |
| GET | `/api/quotation/commodity-prices` | **实时**抓取大宗商品（铜、铝）国际市价 |
| GET | `/api/quotation/commodity-prices/summary` | 大宗商品服务说明 |
| POST | `/api/quotation/feedback-correction` | **误差补偿**：AI 学习报价与结标价偏差并自我修正 |
| POST | `/api/quotation/apply-correction` | 对建议价应用自我修正 |
| GET | `/api/business/idme/health` | iDME 对接状态 |
| GET | `/health` | 健康检查 |

### 绿色生产

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/green/energy-monitoring` | 能效监测：设备功率 → PUE |
| POST | `/api/green/carbon-footprint` | 碳足迹：工时+材料重量 → CO2 |
| POST | `/api/green/energy-optimization` | AI 节能降碳改进报告 |
| GET | `/api/green/dashboard` | 绿色生产仪表盘（减碳趋势图数据） |
| POST | `/api/green/low-carbon-schedule` | 低碳优化排产（遗传算法） |
| POST | `/api/green/product-carbon-footprint` | 产品碳足迹 (PCF) 核算 + 数字碳标签 + AI 改进建议 |
| POST | `/api/green/supply-chain-score` | 绿色供应链评分（环保报告、ISO 14001） |
| POST | `/api/green/procurement-recommendation` | 采购建议（价格+碳强度，黄色预警、环保替代） |
| POST | `/api/green/defect-waste` | 次品浪费折算（物料、能耗、CO2） |
| POST | `/api/green/anomaly-detection` | 异常检测（设备故障苗头，建议停机） |

### 最优生产方案（综合商业分析+绿色环保+智能检测）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/production-plan/optimal` | 生成最优生产方案（JSON） |
| POST | `/api/production-plan/export-excel` | 导出 Excel（含设备、物料、工序、工艺、商业分析、绿色环保、智能检测、流程图，可一键导入） |
| POST | `/api/production-plan/export-pdf` | 导出 PDF（完整产品生产方案） |

## 请求示例

### 利润敏感度分析

```json
POST /api/business/sensitivity
{
  "cost_profit": {
    "bom_cost": 50000,
    "labor_cost": 20000,
    "energy_cost": 5000,
    "selling_price": 100,
    "quantity": 1000,
    "overhead_rate": 0.15,
    "power_fluctuation": 0,
    "logistics_delay_cost": 2000
  },
  "scenarios": [
    {"factor": "bom_cost", "change_percent": 5},
    {"factor": "labor_cost", "change_percent": 5}
  ]
}
```

### 最优生产方案（一键导出 Excel/PDF）

```json
POST /api/production-plan/optimal
{
  "product_name": "PCB 控制板",
  "quantity": 100,
  "cost_profit": { "bom_cost": 5000, "labor_cost": 2000, "energy_cost": 500, "selling_price": 100, "quantity": 100 },
  "equipment": [],
  "materials": [],
  "procedures": [],
  "process_routes": []
}
```

设备/物料/工序/工艺为空时自动生成示例数据。导出 Excel 各 Sheet 表头与前端批量导入格式一致，可分别导入到设备管理、物料管理、工序管理、工艺管理。流程图以 Mermaid 格式包含在 Excel/PDF 中。

### 智能报价完整（逻辑闭环）

```json
POST /api/quotation/full
{
  "material_cost": 5000,
  "labor_cost": 2000,
  "energy_cost": 500,
  "quantity": 100,
  "competitor_price": 85,
  "customer_level": "B",
  "product_name": "工业 PCB",
  "capacity_utilization": 0.35,
  "historical_material_prices": [12.1, 12.3, 12.5, 12.8, 13.0, 12.9]
}
```
返回：`floor_price`、`suggested_price`、`suggested_deal_price`（利润优化）、`material_price_impact`（价格预测）、`expected_profit_margin`、`success_rate_prediction`、`customer_email_wording`、`idme_record_id`、`ui_suggestions`

### 价格预测（AI 子模块）

```json
POST /api/quotation/price-prediction
{
  "material_cost_share": 0.6,
  "current_floor_price": 75,
  "historical_prices": [
    {"date_index": 1, "unit_price": 12.1},
    {"date_index": 2, "unit_price": 12.3},
    {"date_index": 3, "unit_price": 12.5},
    {"date_index": 4, "unit_price": 12.8},
    {"date_index": 5, "unit_price": 13.0}
  ]
}
```

### 利润优化（AI 子模块）

```json
POST /api/quotation/profit-optimization
{
  "base_price": 85,
  "capacity_utilization": 0.35,
  "floor_price": 75
}
```
产能利用率低→降价抢单，产能饱和→提高溢价

### 报价单 PDF 自动生成

```json
POST /api/quotation/generate-pdf
{
  "product_name": "工业 PCB",
  "floor_price": 75,
  "suggested_price": 85,
  "quantity": 100,
  "cost_breakdown": {"material": 5000, "labor": 2000, "energy": 500},
  "valid_days": 30
}
```
返回：PDF 文件下载

### 谈判策略助手

```json
POST /api/quotation/negotiation-strategy
{
  "customer_background": "长期合作伙伴、价格敏感型",
  "suggested_price": 85,
  "floor_price": 75,
  "product_name": "工业 PCB"
}
```
返回：`tactics`（谈判话术）、`max_discount_pct`（最大折扣）、`key_points`（要点）

### 多方案对比

```json
POST /api/quotation/multi-plan-comparison
{
  "product_name": "工业 PCB",
  "floor_price": 75,
  "quantity": 100,
  "material_cost": 5000,
  "labor_cost": 2000,
  "energy_cost": 500,
  "competitor_price": 90
}
```
返回：`plans`（经济型/标准型/高端型，含利润率、中标概率、AI 分析）、`recommendation`

### 大宗商品实时市价（商业实用性）

```
GET /api/quotation/commodity-prices
```
通过 API 实时抓取大宗商品（铜、铝）的国际市价，用于报价成本核算。  
配置 `COMMODITY_API_KEY` 可接入 Metals-API、API Ninjas 等。

### 误差补偿与自我修正（强化学习/反馈闭环）

```json
POST /api/quotation/feedback-correction
[
  {"quoted_price": 85, "actual_deal_price": 82},
  {"quoted_price": 90, "actual_deal_price": 87}
]
```
AI 自动学习历史报价与实际结标价的偏差，并进行自我修正。  
完整报价 `POST /api/quotation/full` 支持 `feedback_records` 参数，自动应用修正。

### 绿色生产

**能效监测** `POST /api/green/energy-monitoring`：
```json
{
  "devices": [
    {"device_id": "D1", "power_kw": 50, "device_type": "process"},
    {"device_id": "D2", "power_kw": 20, "device_type": "auxiliary"}
  ]
}
```
返回：`pue`、`total_power_kw`、`process_power_kw`、`auxiliary_power_kw`

**碳足迹** `POST /api/green/carbon-footprint`：
```json
{
  "production_hours": 8,
  "avg_power_kw": 70,
  "material_weight_kg": 100,
  "material_type": "steel"
}
```
返回：`total_co2_kg`、`energy_co2_kg`、`material_co2_kg`

**仪表盘** `GET /api/green/dashboard?days=30`：返回 `dates`、`carbon_trend`、`pue_trend`、`summary`，供 ECharts 绘制减碳趋势图

**低碳优化排产** `POST /api/green/low-carbon-schedule`：
```json
{
  "electricity_price": {"peak": 1.2, "valley": 0.4, "flat": 0.8},
  "idle_energy_kw": 10,
  "orders": [
    {"order_id": "O1", "duration_hours": 2, "urgency": 5, "deadline_hours": 12, "power_kw": 50},
    {"order_id": "O2", "duration_hours": 3, "urgency": 3, "deadline_hours": 24, "power_kw": 60}
  ],
  "work_start_hour": 8,
  "work_end_hour": 18
}
```
遗传算法在保证交付前提下调整启停顺序，最大化利用谷时。返回 `schedule`、`savings_pct`（对比传统方案节省百分比）

**产品碳足迹 (PCF)** `POST /api/green/product-carbon-footprint`：
```json
{
  "product_id": "P001",
  "product_name": "工业控制器",
  "batch_id": "B001",
  "quantity": 10,
  "parts": [
    {
      "part_id": "PT1",
      "part_name": "外壳",
      "material_source": "铝合金",
      "material_weight_kg": 2.5,
      "material_emission_factor": 8.0,
      "transport_distance_km": 500,
      "processing_hours": 0.5,
      "processing_power_kw": 30
    }
  ],
  "processes": [
    {"process_name": "装配", "duration_hours": 1, "power_kw": 15}
  ]
}
```
返回：`total_co2_kg`、`carbon_label`（数字碳标签）、`high_emission_points`、`ai_suggestions`

### 智能报价 - 底线价格

```json
POST /api/quotation/floor-price
{
  "bom_items": [
    {"material_name": "电阻", "quantity": 100, "unit_price": 0.5, "unit": "个"},
    {"material_name": "PCB", "quantity": 1, "unit_price": 15, "unit": "片"}
  ],
  "man_hours": 2.5,
  "hourly_labor_rate": 50,
  "equipment_depreciation": 80,
  "energy_cost": 25,
  "logistics_cost": 30,
  "quantity": 100,
  "batch_id": "batch_001",
  "bom_id": "bom_001"
}
```

### 市场进入策略

```json
POST /api/business/market-strategy
{
  "product_name": "工业级 PCB",
  "cost_profit": { ... },
  "market_data": {
    "target_industry": "电子制造",
    "user_profile": {"role": "电子厂采购", "industry": "消费电子"},
    "market_penetration_rate": 0.15,
    "competitor_price": 95
  }
}
```

## 与 Java 代理集成

前端可统一通过 Java 代理（8080）转发，或在 `application.yml` 中配置反向代理到本服务 8001 端口。
