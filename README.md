# 🌿 AI 绿能排产 & 碳排推演模拟器

> AI Green Scheduling & Carbon Emission Simulator — MVP v1.0

一款面向制造业的 **AI 绿色排产调度模拟系统**，通过模拟工厂订单与 BOM 物料数据，结合峰谷电价与碳排因子，利用 AI 智能干预排产计划，实现"降本（电费）增效（减碳）"的可视化闭环。

---

## ✨ 功能特性

- 📊 **实时能耗看板** — 工厂近 14 天能耗 & 碳排趋势面积图
- ⚡ **24小时峰谷电价分布** — 红/黄/绿三色直观展示峰平谷时段
- 🤖 **AI 排产优化分析** — 点击订单一键获取 AI 优化方案，含详细推理说明
- 📋 **方案对比弹窗** — 常规方案 vs AI 优化方案数据对比（电费、碳排、能耗）
- ✅ **一键采纳执行** — 采纳后自动更新看板数据，Toast 通知节省金额

---

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Next.js 14 (App Router) + React 18 |
| 样式 | TailwindCSS |
| 图表 | Recharts |
| 图标 | lucide-react |
| 语言 | TypeScript |

---

## 🚀 本地运行

**环境要求：** Node.js >= 18

```bash
# 1. 克隆仓库并切换到本项目分支
git clone https://github.com/explorer-creator/EGS.git
cd EGS
git checkout green-scheduling

# 2. 安装依赖
npm install

# 3. 启动开发服务器
npm run dev
```

打开浏览器访问 [http://localhost:3000](http://localhost:3000)

---

## 📁 项目结构

```
src/
├── app/
│   ├── page.tsx          # 主 Dashboard 页面
│   ├── layout.tsx        # 根布局
│   └── globals.css       # 全局样式
├── components/
│   ├── EnergyChart.tsx   # 能耗碳排趋势面积图
│   ├── PriceChart.tsx    # 24h峰谷电价柱状图
│   ├── OrderModal.tsx    # AI干预详情弹窗
│   └── Toast.tsx         # 操作成功提示组件
└── lib/
    ├── mockData.ts       # Mock数据（订单/电价/趋势）
    └── utils.ts          # 工具函数
```

---

## 📊 核心数据模型

```typescript
// 排产订单优化对比
{
  order_id: "ORD-2026-889",
  original_plan: {
    schedule_time: "14:00-18:00",  // 峰电时段
    est_cost_rmb: 6000,
    est_carbon_kg: 4000
  },
  ai_optimized_plan: {
    schedule_time: "00:00-04:00",  // 谷电时段
    est_cost_rmb: 1500,            // 节省 75%
    est_carbon_kg: 2500            // 减排 37.5%
  }
}
```

---

## 🔗 对接说明

本模块为前端展示层，数据接口兼容以下格式：

- 输入：`ScheduleOrder`（`order_id` / `power_kw` / `duration_hours` / `urgency`）
- 对接方式：替换 [src/lib/mockData.ts](cci:7://file:///c:/Users/zhong/greengeneratesysteam/src/lib/mockData.ts:0:0-0:0) 中的静态数据，或通过 `fetch` 调用后端 API

---

## 👤 作者

**zj** — 绿能排产模块  
部分 EGS 系统前端展示组件
