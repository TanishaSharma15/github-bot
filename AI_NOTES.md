# AI notes

This project was built in Cursor with an AI coding assistant, one chapter at a time. The assistant wrote the Spring Boot code and the tests. The account owner created the GitHub OAuth app, the Slack app, the TiDB database, and the Render service, then checked each chapter in a browser.

## What was decided

- Java 21, Spring Boot, Thymeleaf, and MySQL, so the UI and the API ship in one jar.
- Local MySQL runs in Docker on port 3307 because 3306 was already in use.
- Production uses TiDB Cloud Starter, which speaks MySQL, so the same JDBC driver stays in place. Render hosts the Docker web service. Both are on free plans.
- GitHub will not register a webhook whose URL is `localhost`, so the app was deployed before the connect flow could succeed.
- An opened issue is labeled and posted to Slack only when it matches the saved keyword.
- An opened pull request is labeled when it matches the keyword. When Slack is on, every opened pull request is posted, including one that does not match. Editing or closing a pull request does not create another action.
- A push is posted to Slack when Slack is on. A push is not labeled, because it is not an issue or a pull request.
- Groq is optional. The summary is a separate action for a matching issue or matching pull request. If the key is missing or the call fails, the label and Slack still run. A push does not request a summary.

## What was checked by hand

- `GET /health` returned `{"status":"ok"}` locally and on Render.
- GitHub sign-in landed on the home page as `TanishaSharma15`, and log out returned to the public home page.
- Connecting `TanishaSharma15/code-Editor` from localhost returned GitHub's 422. After deploy, the same connect created a webhook on the public URL.
- A redelivered ping and a new issue both returned 200.
- The first issue deliveries were not saved. Render logs showed `Data too long for column 'payload'`. The column was changed to `LONGTEXT`, the delivery was redelivered, and the issue received a `bug` label and a Slack message.
- The dashboard rule was changed to keyword `note`, label `question`, Slack off. `Just a note` received `question`. `Unrelated change` received no label and was marked `skipped`.
- On `TanishaSharma15/code-Editor`, a pull request titled `Fix test bug` was opened after the pull-request deploy. The account owner confirmed that test.
- A commit pushed to the `chapter-8-bug-test` branch was used to confirm the push alert after that deploy.
- `./mvnw test` passed after each chapter. The latest run reported 50 tests and 0 failures.

## What the assistant did not do

The assistant did not receive the GitHub client secret, the TiDB password, the Slack webhook URL, or a Groq key in the notes that were committed. Those values live in the local `.env`, which is gitignored, and in the Render environment. A Groq account could not be created during the session because the login link was consumed before it could be opened, so the live summary was not exercised. The Groq client is covered by unit tests, and the worker tests show that a failed summary still labels and still notifies Slack.
