# 智能报价模块 - 评委展示要点

## 一、实时性：大宗商品国际市价

**描述**：通过 API 实时抓取大宗商品（如铜、铝）的国际市价，用于报价成本核算。

**实现**：
- 接口：`GET /api/quotation/commodity-prices`
- 支持 Metals-API、API Ninjas 等可配置数据源
- 无 API Key 时使用模拟实时数据（体现架构可扩展性）
- 配置 `COMMODITY_API_KEY` 即可接入真实行情

**商业实用性**：工业品报价与原材料强相关，实时铜铝价格可显著提升报价准确度与竞争力。

---

## 二、误差补偿：AI 自我修正

**描述**：AI 自动学习历史报价与实际结标价的偏差，并进行自我修正。

**实现**：
- 接口：`POST /api/quotation/feedback-correction`、`POST /api/quotation/apply-correction`
- 输入：历史 (quoted_price, actual_deal_price) 对
- 算法：计算平均 actual/quoted 比值，推导修正系数，限制在 ±8% 内
- 完整报价 `POST /api/quotation/full` 支持 `feedback_records` 参数，自动应用修正

**强化学习/反馈闭环**：用实际结标结果持续优化报价建议，体现“从反馈中学习”的闭环思想。
