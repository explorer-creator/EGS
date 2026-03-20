# Git 协作开发指南

本文档说明如何基于 GitHub 私有仓库进行团队协作开发，适用于组长和所有成员。

---

## 一、前置准备

### 1.1 安装 Git

- 下载：https://git-scm.com/downloads
- 安装后打开终端，配置用户名和邮箱（首次使用）：

```bash
git config --global user.name "你的名字"
git config --global user.email "你的邮箱@example.com"
```

### 1.2 加入 GitHub 仓库

- 组长会邀请你加入私有仓库，收到邮件后点击接受
- 确保你有该仓库的 **Write** 权限

---

## 二、组长操作流程

### 2.1 创建私有仓库

1. 登录 GitHub → 右上角 `+` → `New repository`
2. 仓库名填写项目名（如 `egs-project`）
3. 选择 **Private**
4. 点击 `Create repository`

### 2.2 邀请成员

1. 进入仓库 → `Settings` → `Collaborators` → `Add people`
2. 输入成员的 GitHub 用户名或邮箱
3. 赋予 **Write** 权限（可读写、可推送分支）

### 2.3 规划任务（Issues）

1. 进入 `Issues` → `New issue`
2. 每个功能/模块建一个 Issue，示例：

| 标题 | 描述 |
|------|------|
| [模块] 订单管理 - 列表页 | 展示订单列表，支持分页、搜索 |
| [功能] 用户登录 - 记住密码 | 勾选后 7 天内免登录 |
| [修复] 生产方案 405 错误 | 接口返回 405 时的兜底处理 |

3. 成员在 Issue 下评论「我来做」即可认领

### 2.4 审核与合并（Pull Request）

1. 成员提交 PR 后，你会收到通知
2. 打开 PR → 查看 `Files changed` 中的代码变更
3. 有问题：在具体行上添加评论，请求修改
4. 无问题：点击 `Approve`，再点击 `Merge pull request`
5. 合并方式建议选 **Squash and merge**（多个 commit 合并成一个）

### 2.5 保护 main 分支（可选）

1. `Settings` → `Branches` → `Add branch protection rule`
2. 分支名填 `main`
3. 勾选：
   - `Require a pull request before merging`
   - `Require approvals` 设为 1
4. 保存后，main 只能通过 PR 合并，不能直接 push

---

## 三、成员操作流程

### 3.1 克隆仓库（首次）

```bash
git clone https://github.com/你的用户名/仓库名.git
cd 仓库名
```

### 3.2 认领任务后，创建自己的分支

```bash
# 1. 拉取最新 main
git checkout main
git pull origin main

# 2. 创建并切换到功能分支（分支名与任务对应）
git checkout -b feature/订单管理-列表页
```

**分支命名建议：**

- 新功能：`feature/模块名-功能描述`
- 修复 Bug：`fix/问题简述`
- 文档：`docs/xxx`

### 3.3 开发并提交

```bash
# 修改代码后
git add .
git commit -m "feat: 订单列表页基础布局"

# 推送到远程
git push origin feature/订单管理-列表页
```

**Commit 信息规范：**

- `feat: 新功能`
- `fix: 修复 Bug`
- `docs: 文档更新`
- `style: 代码格式（不影响逻辑）`
- `refactor: 重构`

### 3.4 发起 Pull Request

1. 推送后，GitHub 仓库页会提示 **Compare & pull request**
2. 或手动：`Pull requests` → `New pull request`
3. 选择：`base: main` ← `compare: feature/订单管理-列表页`
4. 填写 PR 标题和说明，可写 `Closes #3` 关联 Issue
5. 点击 `Create pull request`

### 3.5 合并后更新本地

```bash
git checkout main
git pull origin main
git branch -d feature/订单管理-列表页
```

---

## 四、常见场景

### 4.1 别人已合并，我还没做完，如何同步 main？

```bash
git checkout main
git pull origin main
git checkout feature/我的分支
git merge main
# 若有冲突，解决后 git add . && git commit
git push origin feature/我的分支
```

### 4.2 提交错了怎么办？

```bash
# 撤销最后一次 commit，保留修改
git reset --soft HEAD~1

# 修改后重新提交
git add .
git commit -m "正确的提交信息"
```

### 4.3 冲突怎么解决？

1. 按 4.1 先 `merge main` 到自己的分支
2. 若提示冲突，打开冲突文件，会看到：
   ```
   <<<<<<< HEAD
   你的代码
   =======
   别人的代码
   >>>>>>> main
   ```
3. 手动保留正确部分，删除 `<<<<<<<`、`=======`、`>>>>>>>` 标记
4. `git add .` → `git commit -m "resolve: 解决与 main 的冲突"` → `git push`

### 4.4 本地项目首次推送到 GitHub

```bash
cd 项目根目录
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/你的用户名/仓库名.git
git push -u origin main
```

---

## 五、流程图速览

```
成员 A:  main → feature/A → 开发 → push → 提 PR
成员 B:  main → feature/B → 开发 → push → 提 PR
组长:    审核 PR → Approve → Merge to main
所有人:  git pull origin main 更新本地
```

---

## 六、注意事项

1. **不要直接在 main 上改代码**，始终在自己的分支开发
2. **合并前先同步 main**，减少冲突
3. **Commit 信息写清楚**，方便回溯
4. **PR 描述写完整**，说明改了啥、为什么改
5. **有疑问在 PR 或 Issue 里讨论**，留痕可查

---

*文档版本：v1.0 | 更新日期：2025-03*
