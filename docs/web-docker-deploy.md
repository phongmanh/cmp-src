# Building and deploying the web app with Docker

How to turn the `webApp` module into a static bundle and serve it from an nginx container.

The output is a plain static site — no server-side runtime. Everything below was verified against
this repo (Compose Multiplatform 1.11.1, Kotlin 2.4.0, nginx 1.31.4-alpine).

---

## 1. Pick a distribution

The `webApp` module can produce three different bundles. All of them land under `webApp/build/dist/`.

| Gradle task | Output directory | Size | Runs on |
|---|---|---|---|
| `:webApp:jsBrowserDistribution` | `dist/js/productionExecutable` | 27 MB | any browser (Kotlin/JS + Skiko wasm) |
| `:webApp:wasmJsBrowserDistribution` | `dist/wasmJs/productionExecutable` | 14 MB | browsers with modern Wasm GC only |
| `:webApp:composeCompatibilityBrowserDistribution` | `dist/composeWebCompatibility/productionExecutable` | 35 MB | **both** — picks at runtime |

**Deploy the compatibility distribution.** Its `webApp.js` is a ~1 KB loader that feature-detects
Wasm GC support and appends the right bundle:

```js
document.body.appendChild(createScript(
    hasSupportOfAllRequiredWasmFeatures() ? "originWasmWebApp.js" : "originJsWebApp.js"))
```

So modern browsers get the small, fast Wasm build and older ones silently fall back to the JS build.
The image is bigger because it carries both, but the *transfer* per visitor is only one of them.

Ship the wasm-only bundle instead if you fully control the client browsers and want a 14 MB image.

---

## 2. Before you build: point the app at the right backend

This is the easiest thing to get wrong, and it fails only after deployment.

`ApiEnvironment.ACTIVE` in `shared/src/commonMain/kotlin/com/liam/cmp_src/core/network/ApiConfig.kt`
is currently `LOCAL`. On the web target `localApiHost()` returns `localhost`, so a `LOCAL` build
asks the browser to call `http://localhost:8080` — **the visitor's own machine**, not your server.
It will also be blocked outright as mixed content once the page itself is served over HTTPS.

Before building a production image:

1. Set `ApiEnvironment.ACTIVE = PRODUCTION`.
2. Set `ApiConfig.PRODUCTION_BASE_URL` to the real backend (it is `https://api.example.com` today).

Then decide how the browser reaches the API:

- **Same origin (recommended).** Proxy the API through the same nginx that serves the app, so no
  CORS configuration is needed at all. Add to the `server` block and set `PRODUCTION_BASE_URL` to
  the page's own origin:

  ```nginx
  location /api/ {
      proxy_pass         http://backend:8080/;
      proxy_set_header   Host $host;
      proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
      proxy_set_header   X-Forwarded-Proto $scheme;
  }
  ```

- **Separate origin.** The backend must send CORS headers for the web app's origin, including on
  preflight, or the browser discards responses before Ktor ever sees them.

The same choice exists in miniature during development, where `compose.dev.yaml` already ships
the proxy — see §5.

---

## 3. Build the distribution

```bash
./gradlew :webApp:composeCompatibilityBrowserDistribution
```

Two things worth knowing about this build:

- **No Android SDK required.** Even though `:shared` has an `androidLibrary` target and the build
  includes `:androidApp`, building the web distribution configures and compiles fine with no
  `ANDROID_HOME` and no `local.properties`. Your CI image does not need the Android SDK.
- **`com.example:api-contract` comes from mavenLocal.** `gradle.properties` sets `useMavenLocal=true`
  and `:shared` depends on `api-contract:1.0.0-SNAPSHOT`, resolved from `~/.m2/repository`. It is a
  KMP publication — the `api-contract-js` and `api-contract-wasm-js` variants must both be present
  or the web build cannot resolve. This is the main obstacle to building inside a clean container
  (see §7).

---

## 4. The image

Three files, all under `webApp/docker/`. The two Compose files and the development nginx
config that sits beside them are covered in §5.

### `webApp/docker/nginx.conf`

```nginx
# http context: adds to the mime table included by /etc/nginx/nginx.conf.
# nginx already maps .wasm -> application/wasm, but has no entry for .mjs, so skiko.mjs and
# js-reexport-symbols.mjs would be served as application/octet-stream and the browser would
# refuse the ES-module imports. This block MUST stay outside `server` — a `types` block inside
# a server or location REPLACES the inherited table instead of extending it.
types {
    application/javascript mjs;
}

server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # Serve the .gz files precompressed at image build time (see Dockerfile).
    gzip_static on;

    location = /healthz {
        access_log off;
        return 200 "ok\n";
    }

    location = /index.html {
        add_header Cache-Control "no-cache" always;
    }

    # Webpack content-hashed wasm — the name changes when the bytes change, so cache forever.
    location ~* "^/[0-9a-f]{16,}\.wasm$" {
        add_header Cache-Control "public, max-age=31536000, immutable" always;
    }

    location / {
        add_header Cache-Control "no-cache" always;
        # No SPA fallback: this app renders into a ComposeViewport and does not route on the URL,
        # so a missing asset must 404 rather than quietly return index.html with a 200.
        try_files $uri $uri/ =404;
    }
}
```

### `webApp/docker/Dockerfile`

```dockerfile
FROM nginx:alpine

ARG DIST=webApp/build/dist/composeWebCompatibility/productionExecutable
COPY ${DIST}/ /usr/share/nginx/html/
COPY webApp/docker/nginx.conf /etc/nginx/conf.d/default.conf

# Drop source maps (they expose the Kotlin sources) and precompress everything gzip_static serves.
RUN find /usr/share/nginx/html -name '*.map' -delete && \
    find /usr/share/nginx/html -type f \
      \( -name '*.js' -o -name '*.mjs' -o -name '*.wasm' -o -name '*.css' \
         -o -name '*.html' -o -name '*.xml' -o -name '*.cvr' \) \
      -exec gzip -9 -k {} \;
```

### `webApp/docker/Dockerfile.dockerignore`

The build context is the repo root (the `COPY` paths above are repo-relative), and the repo is
~2.3 GB with build outputs — without an ignore file, transferring the context alone takes minutes.

```
*
!webApp/docker
!webApp/build/dist/composeWebCompatibility/productionExecutable
```

This cuts the transferred context to ~37 MB.

BuildKit looks for `<dockerfile-path>.dockerignore` before falling back to `.dockerignore` at the
context root, which is why this lives next to the Dockerfile rather than at the repo root. Keeping
it scoped means it applies to *this* image only and cannot interfere with any other Docker build
added to the repo later.

---

## 5. Build and run

Two Compose files, one per environment. Both start with the Gradle step — Docker only packages a
bundle, it never compiles one.

| | `compose.yaml` (production) | `compose.dev.yaml` (development) |
|---|---|---|
| Serves | the bundle baked into the image | `webApp/build/dist/...`, bind-mounted from the host |
| Default bundle | compatibility (Wasm + JS fallback) | the wasm development build |
| Applying a code change | rebuild the image | rerun the Gradle task, reload the page |
| Caching | hashed wasm `immutable`, the rest `no-cache` | `no-store` on everything |
| Compression | `.gz` written at image build, `gzip_static on` | none |
| Source maps | deleted at image build | kept |
| API | your call — see §2 | `/api/` proxied to the host's port 8080 |
| Published on | `8080`, every interface | `8082`, loopback only |
| Compose project | `cmpsrc-web` | `cmpsrc-web-dev` |

They share no project name, port or volume, so both can run at once and neither can be brought up
in the other's mode by accident.

### Production — `webApp/docker/compose.yaml`

```bash
./gradlew :webApp:composeCompatibilityBrowserDistribution
docker compose -f webApp/docker/compose.yaml up -d --wait --build
```

`--wait` blocks until the container's healthcheck reports healthy, so the command failing is a real
signal rather than something you discover later in `docker ps`.

```bash
docker compose -f webApp/docker/compose.yaml ps       # status
docker compose -f webApp/docker/compose.yaml logs -f  # nginx access/error log
docker compose -f webApp/docker/compose.yaml down     # stop and remove
```

```yaml
# Production deployment: the packaged image, serving a bundle baked in at build time.
# Build the bundle first — Compose only packages it:
#
#   ./gradlew :webApp:composeCompatibilityBrowserDistribution
#   docker compose -f webApp/docker/compose.yaml up -d --wait --build
#
# Use compose.dev.yaml to serve a bundle straight out of webApp/build without an image build.
# Everything here is defaulted; set WEB_PORT or WEB_TAG in the environment to override.
#
# Without `name:` the project would be called "docker", after this file's parent directory.
name: cmpsrc-web

services:
  web:
    build:
      # Repo root, because the Dockerfile's COPY paths are repo-relative. BuildKit picks up
      # webApp/docker/Dockerfile.dockerignore automatically and trims the context to ~37 MB.
      context: ../..
      dockerfile: webApp/docker/Dockerfile
    # Tag the release rather than overwriting :latest — WEB_TAG=2026.08.22 docker compose ... build
    image: cmpsrc-web:${WEB_TAG:-latest}
    ports:
      # host:container — nginx listens on 80 inside the container. Published on every interface
      # because the TLS terminator usually runs elsewhere; prefix "127.0.0.1:" when it does not.
      - "${WEB_PORT:-8080}:80"
    restart: unless-stopped
    healthcheck:
      # 127.0.0.1, not localhost: the container resolves localhost to ::1 first, and nginx's
      # `listen 80` binds IPv4 only, so the IPv6 attempt is refused and the check never passes.
      test: ["CMD", "wget", "-qO-", "http://127.0.0.1/healthz"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 5s

  # The API this app talks to is not built from this repo, so there is nothing to wire up by
  # default. To serve both from one origin — which removes CORS entirely — add the service here,
  # add the `location /api/ { proxy_pass http://backend:8080/; ... }` block from §2 of
  # docs/web-docker-deploy.md to nginx.conf, and point ApiConfig.PRODUCTION_BASE_URL at the page's
  # own origin. Compose's network resolves `backend` to the container by service name.
  # compose.dev.yaml does exactly this against a server running on the host.
  #
  # backend:
  #   image: <your-api-image>
  #   restart: unless-stopped
  #   expose:
  #     - "8080"
```

Four things in there are deliberate:

- **`name: cmpsrc-web`** — without it the Compose project is named after this file's parent
  directory, i.e. `docker`, which is both meaningless and easy to collide with.
- **`context: ../..`** — the repo root, because the Dockerfile's `COPY` paths are repo-relative.
  BuildKit still picks up `webApp/docker/Dockerfile.dockerignore` and trims the context to ~37 MB.
- **the healthcheck host** — see the comment above; using `localhost` there yields a container that
  serves traffic perfectly while permanently reporting `unhealthy`.
- **`WEB_TAG` / `WEB_PORT`** — both defaulted, so nothing has to be set. `WEB_TAG=2026.08.22 docker
  compose -f webApp/docker/compose.yaml build` tags a release instead of overwriting `:latest`,
  which matters because every asset but the two hashed `.wasm` blobs keeps a stable filename (§8).

The commented-out `backend` service in the file is the starting point for serving the API from the
same origin (§2), which removes CORS entirely. Compose's default network resolves service names, so
`proxy_pass http://backend:8080/` just works once that service exists.

### Development — `webApp/docker/compose.dev.yaml`

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentExecutableDistribution
docker compose -f webApp/docker/compose.dev.yaml up -d --wait
```

Then open <http://localhost:8082>. There is no `--build`: the service is stock `nginx:alpine` with
the bundle mounted into it, so applying a change is the Gradle task again plus a browser reload,
and `docker compose ... restart` is only needed after editing `nginx.dev.conf`.

The fastest inner loop is still `./gradlew :webApp:wasmJsBrowserDevelopmentRun`, which needs no
container at all. This file is for the two things the webpack dev server cannot show you: the
bundle behind the same nginx configuration production uses, and same-origin calls to a local API.

```yaml
# Development environment: stock nginx serving a bundle built on the host and mounted straight out
# of webApp/build. There is no image to rebuild — run the Gradle task again and reload the page.
# Use compose.yaml when you want the packaged production image.
#
#   ./gradlew :webApp:wasmJsBrowserDevelopmentExecutableDistribution
#   docker compose -f webApp/docker/compose.dev.yaml up -d --wait
#
# The fastest inner loop is still `./gradlew :webApp:wasmJsBrowserDevelopmentRun`, which needs no
# container at all. This file is for the two things that dev server cannot show you: the bundle
# behind the same nginx configuration production uses, and same-origin calls to the local API.
#
# Everything here is defaulted; set WEB_DIST or WEB_PORT in the environment to override.
name: cmpsrc-web-dev

services:
  web:
    # No build stage. The image is only nginx; the site is the mount below.
    image: nginx:alpine
    volumes:
      # Long syntax for create_host_path: false — without it Docker silently creates an empty
      # directory when the bundle has not been built, and the app 404s instead of failing here.
      # Paths are relative to this file's directory. Point WEB_DIST at any other build/dist
      # subdirectory (js/developmentExecutable, composeWebCompatibility/productionExecutable, ...)
      # to serve a different bundle; only the compatibility one carries the JS fallback.
      - type: bind
        source: ${WEB_DIST:-../build/dist/wasmJs/developmentExecutable}
        target: /usr/share/nginx/html
        read_only: true
        bind:
          create_host_path: false
      - type: bind
        source: ./nginx.dev.conf
        target: /etc/nginx/conf.d/default.conf
        read_only: true
    ports:
      # Loopback only: a development bundle is unminified, ships source maps, and is usually built
      # against a local backend, so none of it should be reachable from the network. 8082 because
      # the local API server takes 8080 and its database UI 8081.
      - "127.0.0.1:${WEB_PORT:-8082}:80"
    extra_hosts:
      # Only Linux needs this: Docker Desktop resolves host.docker.internal by itself, Docker
      # Engine does not, and nginx.dev.conf's /api/ proxy resolves the name at startup — so
      # without the entry the container would fail to start there rather than 502 later.
      - "host.docker.internal:host-gateway"
    restart: unless-stopped
    healthcheck:
      # 127.0.0.1, not localhost: the container resolves localhost to ::1 first, and nginx's
      # `listen 80` binds IPv4 only, so the IPv6 attempt is refused and the check never passes.
      test: ["CMD", "wget", "-qO-", "http://127.0.0.1/healthz"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 5s
```

`composeCompatibilityBrowserDistribution` has no development counterpart — only the per-target
`jsBrowserDevelopmentExecutableDistribution` and `wasmJsBrowserDevelopmentExecutableDistribution`
tasks exist — so the default bundle here is the wasm development build, which is also the one that
compiles fastest. `WEB_DIST` points the mount at any other `webApp/build/dist/*` directory:
`../build/dist/composeWebCompatibility/productionExecutable` to exercise the JS fallback path,
`../build/dist/js/developmentExecutable` to work on the JS target alone.

```nginx
# Development counterpart to nginx.conf, mounted by compose.dev.yaml. Three things differ:
# nothing is cached, nothing is compressed, and /api/ is proxied to a server on the host so the
# browser sees a single origin.
#
# http context: adds to the mime table included by /etc/nginx/nginx.conf. nginx already maps
# .wasm -> application/wasm, but has no entry for .mjs, so skiko.mjs and js-reexport-symbols.mjs
# would be served as application/octet-stream and the browser would refuse the ES-module imports.
# .map is here because a development bundle ships source maps. This block MUST stay outside
# `server` — a `types` block inside a server or location REPLACES the inherited table instead of
# extending it.
types {
    application/javascript mjs;
    application/json       map;
}

server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # Off, not `gzip on`: the mounted bundle has no .gz sidecars to serve (the Dockerfile writes
    # those, and this container has no build step), and compressing a ~28 MB development wasm on
    # every reload costs more than sending it uncompressed over loopback.
    gzip off;

    # The point of this file. Filenames are stable across builds, so a cached asset would outlive
    # the Gradle rebuild that replaced it and the browser would keep showing the old app.
    # Inherited by every location below, none of which sets add_header itself.
    add_header Cache-Control "no-store" always;

    location = /healthz {
        access_log off;
        return 200 "ok\n";
    }

    # Same-origin API: point ApiConfig at the page's own origin and no request leaves this origin,
    # so the local server needs no CORS entry for it. Calling http://localhost:8080 directly —
    # what an ApiEnvironment.LOCAL build does today — works too, but that is cross-origin and the
    # server has to allow this app's origin (for the default port in compose.dev.yaml, that means
    # CORS_ALLOWED_ORIGINS=http://localhost:8082).
    #
    # host.docker.internal is the machine running Docker; compose.dev.yaml's extra_hosts entry is
    # what makes it resolvable on Linux. Docker Desktop answers the name with an IPv6 address as
    # well as an IPv4 one and the container has no route to the IPv6 one, so nginx logs an
    # "upstream server temporarily disabled" warning once per 10s fail_timeout window and retries
    # the IPv4 address inside the same request. The proxied request still succeeds — that warning
    # is not a fault to chase.
    location /api/ {
        proxy_pass         http://host.docker.internal:8080/;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }

    location / {
        # No SPA fallback: this app renders into a ComposeViewport and does not route on the URL,
        # so a missing asset must 404 rather than quietly return index.html with a 200.
        try_files $uri $uri/ =404;
    }
}
```

Two differences from the production config carry the intent:

- **`no-store` on everything.** Filenames are stable across builds, so anything the browser cached
  would outlive the rebuild that replaced it — the classic "my change did not appear" of a
  containerized frontend.
- **the `/api/` proxy.** It exists so the local API can be reached from the page's own origin,
  which is the layout §2 recommends for production; without it a `LOCAL` build reaches
  `http://localhost:8080` cross-origin and the server has to allow this app's origin
  (`CORS_ALLOWED_ORIGINS=http://localhost:8082`).

### Without Compose

```bash
./gradlew :webApp:composeCompatibilityBrowserDistribution
docker build -f webApp/docker/Dockerfile -t cmpsrc-web:latest .
docker run --rm -d --name cmpsrc-web -p 8080:80 cmpsrc-web:latest
```

Either way, open <http://localhost:8080>. The resulting image is ~160 MB, of which the
`nginx:alpine` base is ~94 MB.

Precompression matters a lot here — a visitor downloads roughly 4.5 MB gzipped on first load,
whichever bundle they get:

| File | Raw | gzip -9 |
|---|---|---|
| `skiko.wasm` (JS path) | 8.25 MiB | 3.17 MiB |
| `ae9db…wasm` (Wasm path, Skia) | 4.61 MiB | 1.36 MiB |
| `originJsWebApp.js` | 4.44 MiB | 1.17 MiB |
| `originWasmWebApp.js` | 529 KiB | 99 KiB |

If your CDN or edge proxy supports Brotli, serving `.br` alongside will beat these numbers again.

---

## 6. Verify a deployment

```bash
# App loads
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/            # 200

# .mjs MIME type — the JS fallback path breaks silently if this is octet-stream
curl -sI http://localhost:8080/skiko.mjs   | grep -i content-type          # application/javascript

# .wasm MIME type — required for WebAssembly streaming instantiation
curl -sI http://localhost:8080/skiko.wasm  | grep -i content-type          # application/wasm

# Precompression is actually being served
curl -sI -H 'Accept-Encoding: gzip' http://localhost:8080/originJsWebApp.js | grep -i content-encoding

# Missing assets 404 instead of returning index.html
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/nope.js     # 404
```

Then load the page in a browser and confirm the console is clean. A blank page with no console
errors usually means the loader picked a bundle that failed to fetch — check the Network tab for a
404 or a wrong `Content-Type`.

---

## 7. Building entirely inside Docker (CI)

Useful when the build must be reproducible off a developer machine. The blocker is `api-contract`:
`useMavenLocal=true` means it is resolved from `~/.m2/repository`, which is empty in a clean
container. Pick one:

1. **Publish it in an earlier stage** — clone the `api-contract` repo into the builder image and run
   its `./gradlew publishToMavenLocal` before building this project. Most faithful to local dev.
2. **Copy a prepopulated `~/.m2`** — `COPY .m2/repository/com/example /root/.m2/repository/com/example`,
   with the artifacts vendored into the build context or restored from a CI cache.
3. **Publish `api-contract` to a real Maven repository** and drop `useMavenLocal` — the cleanest
   long-term fix, but it changes the project's dependency setup rather than just its packaging.

Sketch, assuming option 2:

```dockerfile
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src

# api-contract, as published by `./gradlew publishToMavenLocal` in that project.
COPY .m2/repository/com/example /root/.m2/repository/com/example

COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY build-logic build-logic
COPY shared shared
COPY webApp webApp
COPY androidApp androidApp
COPY desktopApp desktopApp

RUN ./gradlew --no-daemon :webApp:composeCompatibilityBrowserDistribution

FROM nginx:alpine
COPY --from=build /src/webApp/build/dist/composeWebCompatibility/productionExecutable/ \
                  /usr/share/nginx/html/
COPY webApp/docker/nginx.conf /etc/nginx/conf.d/default.conf
RUN find /usr/share/nginx/html -name '*.map' -delete && \
    find /usr/share/nginx/html -type f \
      \( -name '*.js' -o -name '*.mjs' -o -name '*.wasm' -o -name '*.css' \
         -o -name '*.html' -o -name '*.xml' -o -name '*.cvr' \) \
      -exec gzip -9 -k {} \;
```

Note that this variant needs its own ignore file next to it — e.g. `webApp/docker/Ci.Dockerfile`
plus `webApp/docker/Ci.Dockerfile.dockerignore` — because the one in §4 excludes the sources it
needs to copy. Also:

- **Memory.** `gradle.properties` sets `org.gradle.jvmargs=-Xmx4096M`; give the builder ≥6 GB or the
  Kotlin/JS compile will be killed.
- **Network.** The Kotlin plugin downloads Node, Yarn and Binaryen at build time (see the ivy
  repositories declared in `settings.gradle.kts`). Fully offline builds need those primed.
- **`kotlin-js-store/yarn.lock` is gitignored** (`.gitignore` has `*yarn.lock`), so the container
  regenerates it and npm dependency resolution is not reproducible across builds. Commit that lock
  file if CI reproducibility matters.
- The `COPY androidApp desktopApp` lines are there only because `settings.gradle.kts` includes those
  projects; no Android SDK is needed to configure them for this task.

---

## 8. Serving notes

- **TLS** terminates at whatever sits in front of this container (ingress, ALB, Caddy, Cloudflare).
  The image speaks plain HTTP on port 80 — do not expose it directly to the internet.
- **Cache strategy.** Only the two Skia `.wasm` blobs carry content hashes, so only they get
  `immutable`. Everything else — including `originJsWebApp.js` and `skiko.wasm` — keeps stable
  filenames across releases and is served `no-cache`, meaning the browser revalidates and gets a
  cheap `304` when nothing changed. Version releases by image tag, not by filename.
- **No COOP/COEP headers needed.** Compose's Skiko build does not use `SharedArrayBuffer`, so cross-origin
  isolation is not required.
- **Non-root.** If your platform requires it, swap the base for `nginxinc/nginx-unprivileged:alpine`
  and change `listen 80` to `listen 8080`.
