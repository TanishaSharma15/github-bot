# GitHub Bot

A signed-in GitHub bot that watches a repository you own. An opened issue or pull request that matches a keyword gets a label. Every opened pull request and every push can be posted to Slack. The dashboard shows every delivery and lets you change the keyword, the label, and whether Slack is on.

Live app: [https://github-bot-3t41.onrender.com](https://github-bot-3t41.onrender.com)

Health check: [https://github-bot-3t41.onrender.com/health](https://github-bot-3t41.onrender.com/health)

The free host sleeps after about 15 minutes. The first request after that can take up to a minute. Open the health URL and wait until it returns `{"status":"ok"}` before the rest of the test.

## Slack incoming webhook

The connect page asks for a **Slack incoming webhook URL**. That is not the address in the browser when a Slack channel is open. The app accepts only a URL that starts with `https://hooks.slack.com/`.

Create one in the Slack workspace where you want the alerts:

1. Open [https://api.slack.com/apps](https://api.slack.com/apps) and sign in to Slack.
2. Click **Create New App**, then **From scratch**.
3. Name the app `GitHub Bot`, choose your workspace, and click **Create App**.
4. In the left sidebar, click **Incoming Webhooks**.
5. Turn **Activate Incoming Webhooks** on.
6. Click **Add New Webhook to Workspace**.
7. Choose the channel that should receive the alerts, then click **Allow**.
8. Back on **Incoming Webhooks**, copy the URL under **Webhook URLs for Your Workspace**. It starts with `https://hooks.slack.com/services/...`.

Paste that whole URL into **Slack incoming webhook URL** on the connect page. Keep it private. Anyone with the URL can post into that channel.

To copy an existing URL later, open [https://api.slack.com/apps](https://api.slack.com/apps), select the app, open **Incoming Webhooks**, and copy the same URL.

## Reviewer test

Use your own GitHub account and a repository you administer. The app registers a webhook on that repository, so it needs admin rights there. Create the Slack URL in the section above before connecting a repository.

1. Open the live app and click **Sign in with GitHub**. Authorize the app.
2. Click **Connect repository**. Choose your repository, paste the Slack incoming webhook URL, and click **Connect**.
3. On GitHub, open the repository's **Settings → Webhooks**. The new hook should point at `https://github-bot-3t41.onrender.com/webhooks/github`. Open **Recent Deliveries** and redeliver the ping if it is still red. A successful delivery shows **200**.
4. Open **Dashboard**. Set the keyword to `bug`, the label to `bug`, leave **Post to Slack** checked, and click **Save rule**.
5. Create an issue titled `Login bug on the homepage` and put a sentence in the body.
6. Wait about 30 seconds, then refresh the issue. It should have a `bug` label. Slack should have one message naming the repository, the issue number, and the title.
7. Refresh the dashboard. The new `issues` row should be `queued`, with `label succeeded` and `slack succeeded`.
8. Open a pull request titled `Fix test bug`. Within about 30 seconds it should have a `bug` label. Slack should have one message with the repository, the pull request number, the title, the author, and the pull request link. The dashboard `pull_request` row should show `label succeeded` and `slack succeeded`.
9. Open a pull request titled `Update homepage text`. It should get no label. Slack should still receive one message, and the dashboard row should show `slack succeeded`.
10. Commit a file directly to a branch. Slack should receive one message with the branch, the person who pushed, the commit count, the latest commit message, and a GitHub link. The dashboard `push` row should show `slack succeeded`. A push does not get a label.
11. Change the keyword to `note`, the label to `question`, uncheck **Post to Slack**, and save.
12. Create an issue titled `Just a note`. Within about 30 seconds it should have a `question` label and Slack should stay quiet.
13. Create an issue titled `Unrelated change`. It should get no label. The dashboard row for that delivery should be `skipped`.

A `summary succeeded` action appears only when `GROQ_API_KEY` is set on the host. Without that key, the label and the Slack message still run. That is the intended fallback.

GitHub also sends a second `issues` event after a label is added. Those rows show as `skipped`. The opened issue is the `queued` row. Editing or closing a pull request is also `skipped`. Only the opened pull request creates actions.

## What it does

- Sign in with GitHub. The access token is stored encrypted.
- List repositories you own and register one webhook for issues, pull requests, and pushes.
- Store the Slack URL and the webhook secret encrypted.
- Accept `POST /webhooks/github` with no browser session. The signature is checked against the raw body. Each `X-GitHub-Delivery` is stored once.
- Every 15 seconds, a worker reads new deliveries. An opened issue whose title or body contains the saved keyword gets the saved label, and Slack is posted for that issue only when the rule says so. An opened pull request that matches the keyword gets the same label. When Slack is on, every opened pull request is posted, including one that does not match. A push is posted to Slack when Slack is on, and it is not labeled.
- Each label, summary, and Slack attempt is its own row. A failed attempt retries up to 5 times and does not repeat an attempt that already succeeded.
- The dashboard lists deliveries for the signed-in user and edits that user's rule. The starting rule is keyword `bug`, label `bug`, Slack on.

## Reliability

| Check | Behavior |
|---|---|
| Signature | `X-Hub-Signature-256` is HMAC-SHA256 of the raw body. A bad or missing signature returns 401 and stores nothing. |
| Duplicate delivery | The same `X-GitHub-Delivery` returns 200 and does not insert a second row. |
| Save failure | A database error on a new delivery returns 500 so GitHub retries. |
| Action failure | The failed action is retried on its own. A Slack failure does not add the label again. |
| Summary | An optional Groq summary is added under the Slack message for a matching issue or matching pull request. A missing key, a Groq error, or a push does not block the label or Slack. |
| Secrets | Tokens, webhook secrets, and Slack URLs are encrypted at rest and are not written to logs. `.env` is gitignored. |

## Local setup

Java 21 and Docker are required.

```bash
docker compose up -d
cp .env.example .env
```

Fill `.env` with a GitHub OAuth client id, client secret, and `APP_ENCRYPTION_KEY`. For a local app, the OAuth callback is `http://localhost:8080/login/oauth2/code/github`. The local database is MySQL on port **3307** (`githubbot` / `githubbot`).

```bash
./mvnw test
./mvnw spring-boot:run
```

GitHub rejects `localhost` as a webhook host, so connecting a repository from a local app returns 422. Use the live URL above to test webhooks. `./mvnw test` covers the webhook, the worker, and the dashboard without calling GitHub or Slack.

## Environment

| Name | Purpose |
|---|---|
| `GITHUB_CLIENT_ID` | GitHub OAuth app |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth app |
| `APP_ENCRYPTION_KEY` | Key for tokens, webhook secrets, and Slack URLs |
| `APP_BASE_URL` | Public origin, with no trailing slash. The webhook URL is this origin plus `/webhooks/github` |
| `SPRING_DATASOURCE_URL` | JDBC URL. Production uses TiDB with `sslMode=REQUIRED` |
| `SPRING_DATASOURCE_USERNAME` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `GROQ_API_KEY` | Optional. Enables the summary for a matching issue or pull request |
| `PORT` | Set by Render. Local default is 8080 |

## Layout

| Package | Role |
|---|---|
| `auth` | Sign-in, encrypted token, security rules |
| `repo` | Repository list and webhook registration |
| `webhook` | Signature check and stored deliveries |
| `action` | Rules and the worker that labels issues and pull requests and notifies Slack |
| `slack` | Slack incoming webhook client |
| `summary` | Optional Groq summary |
| `web` | Home, connect page, and dashboard |

The UI is server-rendered Thymeleaf in the same jar. Production runs the `Dockerfile` on Render and stores data in TiDB Cloud (MySQL-compatible). Hibernate updates the schema on startup. The `webhook_deliveries.payload` column must be `LONGTEXT`; a short text column drops real GitHub payloads.

## Tests

`./mvnw test` runs the suite on an in-memory H2 database. It checks public and signed-in pages, token encryption, webhook acceptance and rejection, one-time delivery storage, issue and pull-request labeling, push alerts, Slack retries, dashboard rules, and the Groq response parser.
