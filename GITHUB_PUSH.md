# 推送到 GitHub EGS 仓库

本地已初始化 Git 并完成首次提交。请按以下步骤推送到你的 GitHub EGS 仓库：

## 1. 添加远程仓库

将 `YOUR_USERNAME` 替换为你的 GitHub 用户名：

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

## 3. 添加展示视频（可选）

将展示视频文件放入 `docs/demo/` 目录，然后：

```bash
git add docs/demo/你的视频.mp4
git commit -m "添加展示视频"
git push
```
