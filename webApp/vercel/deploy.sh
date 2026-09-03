#!/usr/bin/env bash
#
# Publishes the web bundle to Vercel as a prebuilt static site.
#
#   ./gradlew :webApp:composeCompatibilityBrowserDistribution
#   webApp/vercel/deploy.sh            # preview deployment
#   webApp/vercel/deploy.sh --prod     # production deployment
#
# Like the Docker files beside it, this only packages a bundle — it never compiles one. Vercel
# cannot build this project itself: `:shared` resolves com.example:api-contract from mavenLocal,
# which does not exist in a Vercel build container, so the bundle is always built on the machine
# that deploys it and uploaded as finished static files.
set -euo pipefail

readonly REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly DIST="${WEB_DIST:-$REPO_ROOT/webApp/build/dist/composeWebCompatibility/productionExecutable}"
readonly STAGE="$REPO_ROOT/webApp/build/vercel"
readonly PROJECT="${VERCEL_PROJECT:-cmp-src}"

if [[ ! -f "$DIST/index.html" ]]; then
    echo "No bundle at $DIST" >&2
    echo "Build one first: ./gradlew :webApp:composeCompatibilityBrowserDistribution" >&2
    exit 1
fi

# The CLI signs in with a short-lived OAuth access token and only renews it while running an
# authenticated command. A deploy started on a token that went stale since the last one dies with
# "Error: Not authorized" — after the upload, with the renewal landing a moment too late to be of
# any use. So spend one cheap authenticated call up front and let it do the renewing.
ensure_signed_in() {
    # Credentials supplied explicitly are the caller's to manage, and `vercel login` would not
    # touch them anyway.
    if [[ -n "${VERCEL_TOKEN:-}" || " $* " == *" --token"* ]]; then
        return
    fi

    if vercel whoami >/dev/null 2>&1; then
        return
    fi

    # Dead rather than merely stale: no refresh token left to spend, so this needs a browser.
    if [[ ! -t 0 ]]; then
        echo "Vercel session has expired, and there is no terminal to sign in from." >&2
        echo "Run: vercel login" >&2
        exit 1
    fi

    echo "Vercel session has expired — signing in." >&2
    vercel login
}

ensure_signed_in "$@"

# Stage rather than deploy $DIST in place: the source maps have to go (they expose the Kotlin
# sources), and vercel.json has to sit at the root of what gets uploaded.
rm -rf "$STAGE"
mkdir -p "$STAGE"
cp -R "$DIST/." "$STAGE/"
find "$STAGE" -name '*.map' -delete
cp "$REPO_ROOT/webApp/vercel/vercel.json" "$STAGE/vercel.json"

# The project link lives at the repo root, where `./gradlew clean` cannot delete it; the staged
# directory is what actually gets deployed, so it needs its own copy.
if [[ -f "$REPO_ROOT/.vercel/project.json" ]]; then
    mkdir -p "$STAGE/.vercel"
    cp "$REPO_ROOT/.vercel/project.json" "$STAGE/.vercel/project.json"
else
    echo "No project link at $REPO_ROOT/.vercel — run: vercel link --yes --project $PROJECT" >&2
    exit 1
fi

echo "Deploying $(du -sh "$STAGE" | cut -f1) from $STAGE"
vercel deploy "$STAGE" "$@"
