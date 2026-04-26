# /pr — 使用 GitHub CLI 提交并创建/更新 PR

你是本仓库的自动化助手。用户通过 `/pr` 调用本指令时，请在**终端中依次执行**下列步骤，使用 **Git** 与 **GitHub CLI（`gh`）** 完成暂存、提交、推送与 PR；不要只给操作建议而不执行命令（除非环境不满足或用户需手动授权）。

## 前置条件

- 已安装 GitHub CLI：`gh --version`
- 已登录：`gh auth status` 成功；若失败，提示用户在本机执行 `gh auth login`，不要伪造凭证。
- 远程托管为 **GitHub**（`gh pr` 针对 GitHub）。若 `git remote` 不是 GitHub，说明无法用 `gh pr create`，停止并告知。

## 默认分支

- PR 合并目标默认 **`main`**。若仓库默认分支为 `master` 或其他，请先 `git remote show origin | sed -n '/HEAD branch/s/.*: //p'` 或 `gh repo view --json defaultBranchRef -q .defaultBranchRef.name` 确认，并用该名作为 `--base`。

## 安全与约束

- **不要**默认使用 `git push --force` / `--force-with-lease`，除非用户明确要求。
- **不要**默认加 `--no-verify`；仅当用户明确要求跳过 hooks 时使用。
- 推送前确认当前分支不是要保护分支上的误操作；若用户在 `main` 上直接开发，提醒其是否应新建分支（可选，不强制替用户建分支，除非用户要求）。

## 工作流（按顺序执行）

### 1. 检查 `gh`

```bash
gh auth status
```

失败则停止，输出明确错误与 `gh auth login` 指引。

### 2. 查看仓库状态

```bash
git status
git rev-parse --abbrev-ref HEAD
```

若工作区无任何可提交变更（无 modified / staged），停止并说明；若有未暂存变更，继续。

### 3. 暂存

- 若用户在对话里**指定了路径**，仅对该路径 `git add <paths>`。
- 否则默认：`git add -A`（或先 `git add -u` 再处理未跟踪文件，以实际状态为准）。

然后执行：

```bash
git diff --staged --stat
git diff --staged
```

### 4. 撰写并提交 Commit message

遵循 **Conventional Commits**：

- 格式：`type(optional-scope): subject`（**subject 用英文**，祈使语气，≤72 字符，句末无句号）
- `type`：`feat` | `fix` | `docs` | `style` | `refactor` | `perf` | `test` | `build` | `ci` | `chore`
- `scope` 可与模块对应，例如：`agent`、`rag`、`milvus`、`api`、`config`、`controller` 等
- 需要时加 **body**（可用中文说明背景）；需要关联 issue 时在 footer 写 `Fixes #123` / `Refs #456`

单行提交：

```bash
git commit -m "type(scope): short English subject"
```

多行正文（推荐用临时文件避免引号问题）：

```bash
git commit -F /path/to/commitmsg.txt
```

其中 `commitmsg.txt` 第一行为 subject，空行后为 body/footer。

### 5. 推送当前分支

```bash
git push -u origin HEAD
```

若已设置 upstream 且无需改远程名，可用 `git push`。

推送失败时：根据错误信息处理（如未设置 remote、无权限、需拉取先合并）；**不要**擅自 force push。

### 6. 已有 PR 还是新建 PR

先查当前分支是否已有 PR：

```bash
gh pr list --head "$(git rev-parse --abbrev-ref HEAD)" --state all --json number,url,state --limit 5
```

或使用：

```bash
gh pr view --json number,url,state 2>/dev/null
```

- **若已存在 open 的 PR**：根据用户意图执行 `gh pr edit <number> --title "..." --body-file ...` 更新标题/描述，或仅输出 PR 链接；**不要**再 `gh pr create` 重复创建。
- **若不存在**：创建 PR（见下一节）。

### 7. 创建 PR（`gh pr create`）

默认合并进 **`main`**（与「默认分支」一节一致）：

```bash
gh pr create --base main --title "type(scope): concise title in English" --body-file /path/to/pr-body.md
```

**`pr-body.md` 必须使用下列 Markdown 结构**（占位符替换为实际内容）：

```markdown
## 摘要

（1～3 句话：做什么、为什么）

## 变更说明

- 
- 

## 测试

- [ ] `mvn -q -DskipTests compile`
- [ ] （补充手动验证场景或「无」）

## 破坏性变更

无

## 关联事项

无 / Closes # / Refs #
```

若用户更希望用本地 commit 信息填充草稿，可改用：

```bash
gh pr create --base main --fill
```

并在回复中说明可再手动编辑 PR 描述以符合上表结构。

成功后在回复中贴出 **`gh pr view --web` 输出的 URL** 或 `gh pr view` 的链接。

## 权限说明

若沙箱或环境阻止网络/ git 写入，说明失败原因，并提示用户在终端自行执行相同命令或在本机重试。

## 小结

用户期望：**一条 `/pr` 指令 → 你实际跑通 `git add` → `git commit` → `git push` → `gh pr create`（或 `gh pr edit`）**，并统一 commit/PR 文案风格如上。
