# Deploying the web app to Vercel

How to publish the `webApp` bundle to Vercel as a static site. Companion to
`web-docker-deploy.md`, which covers the self-hosted nginx path; §1 and §2 of that document —
choosing a distribution and pointing the app at the right backend — apply here unchanged.

Verified against Vercel CLI 59.11.2, project `cmp-src` (`https://cmp-src.vercel.app`).

---

## 1. Vercel cannot build this project

The bundle is always built on the machine that deploys it and uploaded as finished static files.
Vercel's build container has no way to produce it:

- `:shared` resolves `com.example:api-contract:1.0.0-SNAPSHOT` from **mavenLocal**, which only
  exists on a developer machine. This is the same obstacle §7 of the Docker document describes for
  building inside a clean container.
- `webApp/build/` is git-ignored, so a Git-triggered deploy would find nothing to serve.

So there is no `buildCommand` and no Git integration. `webApp/vercel/deploy.sh` uploads a
directory, exactly like the Dockerfile copies one.

## 2. Deploy

```bash
./gradlew :webApp:composeCompatibilityBrowserDistribution
webApp/vercel/deploy.sh            # preview deployment
webApp/vercel/deploy.sh --prod     # production deployment
```

The script stages `webApp/build/dist/composeWebCompatibility/productionExecutable` into
`webApp/build/vercel/`, deletes the source maps (8 MB of the 36 MB, and they expose the Kotlin
sources), drops `vercel.json` at the root of the upload, and hands the result to `vercel deploy`.
Anything after the script name is passed straight through to the CLI.

Staging rather than deploying the distribution in place is what makes the map deletion safe — the
Gradle output is never modified, so the next build is not confused by a half-emptied directory.

The project link lives in `.vercel/project.json` at the **repo root**, where `./gradlew clean`
cannot delete it; the script copies it into the staged directory each run. First-time setup:

```bash
vercel link --yes --project cmp-src
```

Credentials look after themselves. The CLI signs in with a short-lived OAuth access token and only
renews it while running an authenticated command, so a deploy started on a token that went stale
since the last one fails with `Error: Not authorized` — having renewed the token a moment too late
to use it. The script spends one `vercel whoami` before staging to force that renewal early. A
session that is dead rather than merely stale has no refresh token left to spend, so it falls back
to `vercel login`, or exits telling you to run it when there is no terminal to sign in from.
`VERCEL_TOKEN` in the environment or `--token` on the command line skips the check entirely.

## 3. `webApp/vercel/vercel.json`

Two header rules, mirroring `nginx.conf`:

| Path | `Cache-Control` | Why |
|---|---|---|
| everything | `no-cache` | Filenames are stable across builds, so a cached copy would outlive the deploy that replaced it |
| `/<16+ hex>.wasm` | `public, max-age=31536000, immutable` | Webpack content-hashes these — the name changes when the bytes do |

The rules overlap, and the hashed-wasm rule is declared **second** on purpose: for a repeated
header key the last matching rule wins. `skiko.wasm` keeps a stable name and correctly stays
`no-cache`, because the pattern requires 16+ hex characters.

Three things nginx needed that Vercel does not:

- **No `types` block.** Vercel already serves `.mjs` as `application/javascript` and `.wasm` as
  `application/wasm`. The nginx gotcha that would otherwise break `skiko.mjs` does not apply.
- **No precompression.** Vercel compresses at the edge, so there is no `gzip -9 -k` step and no
  `gzip_static`.
- **No `try_files`.** Vercel's static serving already 404s an unknown path, which is what this app
  wants — it renders into a `ComposeViewport` and does not route on the URL, so an SPA fallback
  would turn a missing asset into a `200` with `index.html` in it.

There is no `/healthz` either: there is no container to health-check.

## 4. CORS is the thing that breaks

The bundle is served from `https://cmp-src.vercel.app` and calls the API at
`ApiConfig.PRODUCTION_BASE_URL` — a **different origin**. Unlike the Docker setup, there is no
nginx in front to proxy `/api/` onto the same origin, so the backend must allow this origin
explicitly, on the preflight as well as the request.

The failure is silent from the app's side: the browser discards the response before Ktor sees it,
so a sign-in just fails. Check it from the command line rather than guessing — a rejected origin
gets `403` with no `access-control-allow-origin`, while an allowed one gets the real status:

```bash
curl -sS -X POST "$API/api/v1/auth/login" \
  -H "Origin: https://cmp-src.vercel.app" -H "Content-Type: application/json" \
  -d '{"email":"probe@example.com","password":"wrong"}' -D - -o /dev/null
```

Add the origin to the backend's `CORS_ALLOWED_ORIGINS` and redeploy it.

## 5. Verifying a deployment

Preview URLs are covered by Deployment Protection, so plain `curl` gets a `302` to a login page.
`vercel curl` mints a bypass token automatically:

```bash
vercel curl <preview-url>/skiko.mjs -- -sSD - -o /dev/null
```

The production domain is public, so `curl` is enough there. Worth checking after a config change:
`.mjs` and `.wasm` content types, `Cache-Control` on a hashed wasm versus `skiko.wasm`, a `404` on
`/originJsWebApp.js.map` (proving the maps were stripped) and on an unknown path (proving there is
no SPA fallback).

In the browser the page shows the `index.html` spinner, then the Compose UI paints — but **text
appears a second or two after the layout does**, once Skiko has fetched its fonts. A screenshot
taken too early shows an empty-looking form and is not a bug.
