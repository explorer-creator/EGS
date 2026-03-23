# 真实第三方 API 配置说明

以下任选一种方式（**不要**把密钥写进仓库里的 `application.yml`）：

1. **推荐**：在 `proxy/src/main/resources/` 下复制 `application-local.yml.example` 为 **`application-local.yml`**，填写密钥（该文件已列入 `.gitignore`）。项目已配置 `spring.config.import` 自动合并该文件。
2. **或**：在 **PowerShell** 里设置环境变量后启动 `proxy`（见下表）。

| 变量 | 作用 |
|------|------|
| `AMAP_KEY` | **高德 Web 服务 Key**，驾车路径、ETA 路段耗时（与 `amap.key` 对应） |
| `DASHSCOPE_API_KEY` | **通义千问**（DashScope OpenAI 兼容接口），产品问答、机器人对话 |
| `XDM_BASE_URL` | xDM-F 服务地址（设备/物料等动态 API），不配则按 `application.yml` 默认 |

## 高德地图 Key（AMAP_KEY）怎么获得

1. 打开 [高德开放平台控制台](https://console.amap.com/)。
2. **应用管理 → 我的应用 → 创建新应用**（名称随意，如「EGS 本地」）。
3. 在该应用下 **添加 Key**：
   - **Key 名称**：随意；
   - **服务平台**：选 **Web 服务**（服务端调用驾车路径、地理编码等；不要用「Web端(JS API)」单独当服务端 Key，需 Web 服务类型）。
4. 创建成功后复制 **Key** 字符串，写入 `application-local.yml` 的 `amap.key:`，或执行：
   ```powershell
   $env:AMAP_KEY="你复制的Key"
   ```
5. **安全设置**（控制台里该 Key 的「设置」）：若仅本机调试，可将 **IP 白名单** 设为 `127.0.0.1`；或按高德说明配置域名/Referer。
6. 重启 Java 代理。可访问 `GET http://localhost:8080/api/map/config`（若项目中有该接口），或看 TMS ETA 是否不再提示「未配置高德 Key」。

## 通义千问 Key（DashScope）

1. 登录 [阿里云 DashScope 控制台](https://dashscope.console.aliyun.com/apiKey) 创建 **API-KEY**（形如 `sk-...`）。
2. 写入 `application-local.yml` 的 `qwen.api-key:`，或：
   ```powershell
   $env:DASHSCOPE_API_KEY="sk-你的密钥"
   ```
3. **模块 A 配图**（`EGSfb/A/server.py`）使用环境变量 **`DASHSCOPE_API_KEY`**，与 Java 共用同一 Key 即可：
   ```powershell
   cd EGSfb\A
   $env:DASHSCOPE_API_KEY="sk-你的密钥"
   python server.py
   ```

## ETA / 运输地图（已接真实 HTTP）

- **Open-Meteo**：无需 Key，服务端需能访问 `https://api.open-meteo.com`（HTTPS）。
- **独立天气查询（演示）**：`GET /api/weather/open-meteo/current?latitude=31.23&longitude=121.47`  
  与 `EtaRealtimeService#fetchOpenMeteoCurrent` 同源；前端「智慧 TMS」页顶部可填经纬度试查。
- **高德**：未配置 `AMAP_KEY` 时，ETA 仍可用 Open-Meteo 天气，但**路况**会降级为球面距离估算，并在接口 `warnings` 中说明。

## 千问 LLM

`application.yml` 中 `qwen.api-key` 已改为仅允许 `${DASHSCOPE_API_KEY:}`，**请勿把 Key 写进仓库**。未配置时调用 `/api/llm/*` 会返回明确错误信息。

## 调用失败时如何排查

1. 浏览器 **Network** 查看响应 JSON 中的 `message`。
2. 代理未启动：前端会提示「请求未到达服务器」。
3. Open-Meteo 超时/防火墙：ETA 接口返回 `success: false` 及原因。
