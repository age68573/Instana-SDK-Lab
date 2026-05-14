# AGENTS.md

本文件提供給 Codex / AI coding agent 使用，目標是開發一個可部署在 JBoss EAP 的 Java 8 Web Application，用來展示 Instana Java SDK 的 method-level tracing、tag、fake query 與錯誤模擬能力。

---

## 1. Git 工作流

### 基本規則

- 每完成一個獨立功能後，必須執行：

```bash
git add .
git commit -m "<type>(<scope>): <description>"
```

### GitHub 推送與 PR

- 未經使用者明確要求，不自動 push 到遠端分支。
- 若使用者明確要求推送，必須確認或使用使用者指定的 remote 與 branch，例如 `origin feature/example`。
- 若使用者明確要求建立 PR，使用 GitHub CLI 或 GitHub connector 建立 draft PR，並確認目標分支，例如 `main` 或 `develop`。
- 推送或建立 PR 前，應先確認本機已有可用的 GitHub 認證，例如 SSH key、GitHub token，或已登入的 `gh` CLI。
- 完成功能後若使用者要求自動 commit / push / PR，建議流程如下：

```bash
mvn test
git add .
git commit -m "<type>(<scope>): <description>"
git push <remote> <branch>
gh pr create --draft --base <base-branch> --head <branch> --title "<title>" --body "<body>"
```
