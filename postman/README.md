# API smoke test (Postman / Newman)

End-to-end smoke test of the URL Shortener API. It exercises the full flow against a
**running instance**:

1. **Register** a user (subscription) — tolerates `409` so re-runs are idempotent
2. **Login** — captures the issued JWT into a collection variable
3. **Create without token** — asserts the write endpoint is protected (`401`)
4. **Create with token** — shortens a URL (`201`) and captures the short key
5. **Resolve** the short key back to the original URL (`200`, public)
6. **List** the shortened URLs (`200`, public)

## Files

- `url-shortener.postman_collection.json` — the collection (requests + assertions)
- `local.postman_environment.json` — environment (`baseUrl`, credentials, URL to shorten)

## Prerequisites

- A **running instance** of the API (default `http://localhost:8080`):
  ```bash
  mvn spring-boot:run -Dspring-boot.run.profiles=local
  ```
- To run from the CLI: **Node.js 18+** (provides `npm` / `npx`) — <https://nodejs.org>.
  No Node is needed if you run the collection from the Postman app instead.

The register step tolerates `409`, so the collection is **idempotent**: re-running it when the
user already exists still passes.

## Run in the Postman app

Import both files, select the *URL Shortener - local* environment, then **Run collection**.

## Run from the CLI (Newman)

```bash
# no global install needed — npx fetches Newman on the fly:
npx --yes newman run postman/url-shortener.postman_collection.json \
  -e postman/local.postman_environment.json

# ...or install it once and call it directly:
npm install -g newman
newman run postman/url-shortener.postman_collection.json \
  -e postman/local.postman_environment.json
```

Newman exits non-zero if any assertion fails, so it can gate a CI pipeline.

Override the target host without editing files:

```bash
newman run postman/url-shortener.postman_collection.json \
  -e postman/local.postman_environment.json \
  --env-var baseUrl=https://your-deployed-host
```
