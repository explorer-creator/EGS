# GitHub Pages 纯静态演示（无 Java / 无 8003）

在 **不上云、不内网穿透** 的前提下，可用本仓库构建 **仅前端的静态站**，接口在浏览器内用与后端 Mock **同源** 的演示数据（`staticDemoData.js` / `XdmMockService` 一致），供评委点击链接体验。

## 访问地址

仓库名为 `EGS`、使用默认 `VITE_BASE=/EGS/` 时，发布成功后一般为：

`https://<你的GitHub用户名>.github.io/EGS/`

若修改了 `front/.env.demo` 中的 `VITE_BASE`，请按实际子路径访问。

## 本地构建与预览

```bash
cd front
npm install
npm run build:demo
npm run preview:demo
```

浏览器打开终端提示的地址（本地预览时子路径与 `VITE_BASE` 一致）。

## 启用 GitHub Pages（任选其一）

### 方式 A：Actions 自动发布到 `gh-pages` 分支

1. 推送包含 `.github/workflows/deploy-egs-demo-pages.yml` 的 `main`。
2. 在仓库 **Settings → Pages**：**Source** 选 **Deploy from a branch**，Branch 选 **`gh-pages`** / **`/ (root)`**。
3. 等待 Actions 跑完，几分钟后可用上文的 `github.io` 链接访问。

### 方式 B：手动上传 `dist`

```bash
cd front
npm run build:demo
```

将 `front/dist` 内容作为静态站点托管到任意支持纯静态的托管（含 GitHub Pages 的其它接入方式）。

## 配置说明

| 文件 | 作用 |
|------|------|
| `front/.env.demo` | `VITE_STATIC_DEMO=true`、`VITE_BASE=/EGS/` |
| `front/src/staticDemoApi.js` | 拦截 `/api`，返回演示 JSON |
| `front/src/staticDemoData.js` | 设备/物料/工序/路线等快照 |

登录演示可用 **`admin` / `admin123`**（与本地 Mock 一致）。大模型、RAG、高德地图等仍显示 **静态占位文案**，完整能力需在本地启动 `proxy` 并配置 Key。

## 私有仓库说明

在 **GitHub 免费私有仓库** 下，**公开**的 `github.io` 站点可能受限；若无法开启 Pages，可将仓库改为 **Public**，或使用 **组织/付费** 套餐中的 Pages 能力。
