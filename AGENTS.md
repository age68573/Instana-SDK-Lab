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

---

## 2. 專案目標

開發一個 Java Web Application，用來展示 Instana Java SDK 的 tracing 能力。

此應用程式需要：

- 使用 Java 8。
- 使用 Maven。
- 產出 WAR 檔案。
- 可部署到 JBoss EAP。
- 使用 JAX-RS / Servlet 提供 Web API。
- 使用 Instana Java SDK 標記 method-level trace。
- 可以在 Instana UI 中看到各個主要 method 的執行時間。
- 不使用外部資料庫。
- 使用 H2 in-memory database 或假資料模擬 query。
- 可模擬慢查詢、業務錯誤、系統錯誤、隨機錯誤。
- 使用 Instana tag 加上 order id、customer id、scenario、error type 等資訊。

---

## 3. 技術限制

### Java

- Java version：Java 8。
- Source / Target：1.8。
- 不可使用 Java 11+ 語法。
- 不可使用 `jakarta.*`。
- 必須使用 `javax.*`。

### Application Server

- 最終部署目標：JBoss EAP。
- Packaging：WAR。
- 不要使用 Spring Boot embedded Tomcat。
- 不要假設應用程式會用 embedded server 執行。

### Database

- 不架設外部資料庫。
- 可以使用 H2 in-memory database。
- Query 行為應盡量透過 JDBC 模擬，讓 Instana 有機會看到 JDBC / DB 類 trace。
