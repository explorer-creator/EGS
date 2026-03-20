# 产品制造知识问答 - 千问 API 配置说明

## 功能说明

「产品制造问答」模块基于阿里云通义千问（Qwen）大模型，用户输入产品名称（如耳机、充电宝等），模型将输出：

- **原材料**：生产该产品所需的主要原材料
- **所用设备**：生产过程中使用的主要设备
- **工序**：主要工序步骤
- **工艺流程**：完整的工艺流程描述
- **相关物理原理**：涉及的主要物理原理（力学、热学、电磁学等）

## 配置步骤

### 1. 获取千问 API Key

1. 登录 [阿里云百炼控制台](https://bailian.console.aliyun.com/)
2. 进入「API-KEY 管理」
3. 创建或复制已有的 API Key（格式类似 `sk-xxx`）

### 2. 配置 API Key

**方式一：环境变量（推荐）**

```bash
# Windows PowerShell
$env:DASHSCOPE_API_KEY = "sk-你的API密钥"

# Linux / macOS
export DASHSCOPE_API_KEY="sk-你的API密钥"
```

**方式二：application.yml**

在 `EGSfb/proxy/src/main/resources/application.yml` 中配置：

```yaml
qwen:
  api-key: sk-你的API密钥
  base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
  model: qwen-plus
```

> 注意：不要将 API Key 提交到版本控制系统，建议使用环境变量。

### 3. 可选配置

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| API Key | `DASHSCOPE_API_KEY` | - | 必填 |
| 基础地址 | `QWEN_BASE_URL` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 国内北京地域 |
| 模型 | `QWEN_MODEL` | `qwen-plus` | 可选 `qwen-turbo`（更快）、`qwen-max`（更强） |

## 使用方式

1. 启动后端服务（端口 8080）
2. 启动前端（`npm run dev`，端口 5173）
3. 登录系统后，点击顶部导航「**产品制造问答**」
4. 在输入框中输入产品名称（如：耳机、充电宝、锂电池、行星减速器）
5. 点击「查询」或按回车，等待 AI 生成回答

## API 接口

- **路径**：`POST /api/llm/product-query`
- **请求体**：`{ "product": "产品名称" }`
- **响应**：`{ "success": true, "content": "AI 回答内容", "product": "产品名称" }`

## 故障排查

- **「请配置千问 API Key」**：检查环境变量或 application.yml 中的 `qwen.api-key`
- **「调用千问 API 失败」**：检查网络、API Key 是否有效、阿里云账户余额
- **403 鉴权失败**：确认 API Key 正确，且百炼平台已开通对应模型
