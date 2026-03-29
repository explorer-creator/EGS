# 推送到 GitHub EGS 仓库

本地已初始化 Git 并完成首次提交。请按以下步骤推送到你的 GitHub 仓库：

## 1. 添加远程仓库（若尚未添加）

当前仓库若已配置 `origin`，可跳过本节。否则将 `YOUR_USERNAME` 替换为你的 GitHub 用户名：

```bash
cd D:\EGS\EGSfb
git remote add origin https://github.com/YOUR_USERNAME/EGS.git
```

如果仓库在组织下，例如：

```bash
git remote add origin https://github.com/组织名/EGS.git
```

## 2. 推送到 GitHub

```bash
git branch -M main
git push -u origin main
```

## 3. 评委如何体验网页

本作品是 **Vue 前端 + Spring Boot 代理（+ 可选 xDM-F 8003）**，不是纯静态页，**GitHub 仓库本身不会自动提供一个可点击的完整在线演示地址**。评委可用下面任一方式体验：

### 方式 A：克隆后本地运行（推荐，无需 8003）

1. 克隆仓库：`git clone https://github.com/你的账号/EGS.git`
2. **保持默认 Mock**：环境变量 `XDM_MOCK_MODE` 默认为 `true`（或在 `proxy/.../application.yml` 中 `xdm.mock-mode: true`），**不需要**本机启动 xDM-F（8003）。
3. 按根目录 [README.md](../../README.md)「快速启动」：先起 `proxy`（8080），再起 `front`（5173），浏览器访问 `http://localhost:5173`，登录可用 `admin` / `admin123`（Mock 说明见 [作品安装说明](../作品安装说明.md)）。

若有人把 `mock-mode` 设成了 `false` 却 **没有** 开 8003，代理在启动时会 **自动改回 Mock**，避免整站不可用。

### 方式 B：部署到公网（阿里云 / 其他云）

将前端静态资源与 Java 代理按 [阿里云部署指南](../阿里云部署指南.md) 或 [上云部署参考](../上云部署参考.md) 部署后，把 **公网访问 URL** 写在仓库 README 或提交说明里，评委即可在浏览器中打开。部署环境若同样 **无 8003**，请设置 `XDM_MOCK_MODE=true` 或依赖上述自动降级逻辑。

### 方式 C：仅展示视频（可选）

将展示视频放入 `docs/demo/`（大文件建议用 Git LFS 或外链，见仓库 `.gitignore` 说明），在 README 中附上链接。

## 4. 添加展示视频（可选）

```bash
git add docs/demo/你的视频.mp4
git commit -m "添加展示视频"
git push
```
