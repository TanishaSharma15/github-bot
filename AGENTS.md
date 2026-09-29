# Agent notes

This is a Java 21 Spring Boot app in package `com.githubbot`. The UI is Thymeleaf. Tests use H2. Local MySQL is Docker on port 3307. Production is the `Dockerfile` on Render and a MySQL-compatible TiDB database.

Run `./mvnw test` before calling a change done. Do not start the app from the home directory; the Maven wrapper and `docker-compose.yml` are in this folder.

## Do not break these

- Verify GitHub webhooks with HMAC-SHA256 over the raw request bytes. Parsing JSON first can change the bytes and reject a real delivery.
- `X-GitHub-Delivery` is unique. A repeat returns 200 and does not insert another row. A failed insert of a new delivery returns 500 so GitHub retries.
- `/webhooks/github` is public and is excluded from CSRF. Everything else except `/`, `/health`, `/error`, static files, and the OAuth paths requires a GitHub login.
- Label, summary, and Slack are separate `bot_actions` rows. A retry must not repeat an action whose status is `succeeded`.
- The worker reads the repository owner's saved rule. The default, when no row exists, is keyword `bug`, label `bug`, Slack on.
- An opened issue is labeled and posted to Slack only when the keyword matches. An opened pull request is labeled only when the keyword matches, but it is posted to Slack whenever Slack is on. A push is posted to Slack when Slack is on and is never labeled.
- A summary action is created only for a matching issue or matching pull request, and only when Slack is on and `GROQ_API_KEY` is set. A push never creates a summary.
- `webhook_deliveries.payload` is `LONGTEXT`. A shorter type drops real GitHub issue bodies. Hibernate `ddl-auto: update` does not widen an existing column; alter it in TiDB if the table was created earlier.
- Encrypt GitHub tokens, webhook secrets, and Slack URLs with `TokenCipher`. Do not log those values, the Groq key, or the signature header.
- Do not commit `.env`. `.env.example` stays empty of real secrets.

## Where to change behavior

| Change | Start here |
|---|---|
| Sign-in or public paths | `auth/SecurityConfig.java`, `auth/GitHubLoginSuccessHandler.java` |
| Connecting a repository | `repo/RepositoryConnectionService.java` |
| Receiving a webhook | `webhook/WebhookReceiver.java`, `webhook/WebhookSignatures.java` |
| Keyword, label, Slack, summary | `action/BotActionService.java`, `action/IssueMatch.java`, `action/PullRequestMatch.java`, `action/PushMatch.java` |
| Dashboard | `web/DashboardController.java`, `templates/dashboard.html` |
| Groq | `summary/GroqSummaryClient.java` |

The worker runs on `app.worker-delay-ms` (15 seconds in production). Tests set that delay to one hour so the scheduler does not fire during `./mvnw test`. Call `BotActionWorker.process()` from a test instead.

A new child table needs its rows deleted before `users` or `repositories` in test setup. The H2 database is shared by the Spring tests.
