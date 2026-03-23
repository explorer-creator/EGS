# 与主站的关系

主站左侧菜单 **「绿能排产演示（队友）」** 通过 **iframe** 嵌入本目录运行后的页面（默认 `http://localhost:3000`）。

请先在本目录执行：

```bash
npm install
npm run dev
```

主站不再重复实现本演示的 UI；可选环境变量 `VITE_GREEN_DEMO_URL` 见 `EGSfb/front/.env.example`。

Java 侧仍保留 `GET /api/green/scheduling-dashboard` 等接口供需要时调用，与 iframe 无强依赖。
