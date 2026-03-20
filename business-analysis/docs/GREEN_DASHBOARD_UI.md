# 绿色生产仪表盘 - UI 展示说明

## 数据接口

```
GET /api/green/dashboard?days=30
```

## 返回结构

```json
{
  "dates": ["2025-02-15", "2025-02-16", ...],
  "carbon_trend": [1200, 1185, 1170, ...],
  "pue_trend": [1.8, 1.78, 1.76, ...],
  "summary": {
    "latest_co2_kg": 950,
    "latest_pue": 1.45,
    "reduction_pct": 20.8
  },
  "chart_hint": {
    "carbon_trend": "ECharts 折线图：xAxis=dates, yAxis=carbon_trend, 展示减碳趋势",
    "pue_trend": "ECharts 折线图：xAxis=dates, yAxis=pue_trend, 展示 PUE 能效趋势"
  }
}
```

## ECharts 配置示例

### 减碳趋势图

```javascript
option = {
  title: { text: '减碳趋势' },
  xAxis: { type: 'category', data: data.dates },
  yAxis: { type: 'value', name: 'CO2 (kg)' },
  series: [{ type: 'line', data: data.carbon_trend, smooth: true }]
};
```

### PUE 能效趋势图

```javascript
option = {
  title: { text: 'PUE 能效趋势' },
  xAxis: { type: 'category', data: data.dates },
  yAxis: { type: 'value', name: 'PUE', min: 1 },
  series: [{ type: 'line', data: data.pue_trend, smooth: true }]
};
```

### 仪表盘卡片

- 当日 CO2：`summary.latest_co2_kg` kg
- 当前 PUE：`summary.latest_pue`
- 累计减碳：`summary.reduction_pct` %
