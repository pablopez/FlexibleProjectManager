# Deployment guide

## Prerequisites

Local SQLite development requires Java 21, Maven, Node.js and pnpm. Container
verification requires Docker with Compose support. Production-style PostgreSQL
deployment uses PostgreSQL 16 or later and an externally managed key directory.

## Local SQLite

Run `cd apps/backend && mvn spring-boot:run`. The default `local` profile stores
the database at `~/.flexible-project-manager/flexible-project-manager.sqlite`.
Set `FPM_DATABASE_PATH` to choose another location. Start the web app with
`cd apps/web && pnpm install && pnpm dev`; Vite proxies `/api` to port 8080.

## PostgreSQL and Compose

`docker compose up --build` starts PostgreSQL, the backend and the frontend.
The example database password is local-only and must not be reused. The frontend
is available at <http://localhost:8081>, and its same-origin `/api` requests are
proxied by nginx to the backend. This topology preserves HttpOnly refresh
cookies and avoids production browser CORS.

For a separately hosted frontend, set `FPM_ALLOWED_ORIGIN` to the exact HTTPS
origin (never `*`) and provide the matching `VITE_API_BASE_URL` at build time.
Credentials remain enabled and refresh cookies use `SameSite=Strict`.

## Production configuration

Activate the `prod` profile. It includes the PostgreSQL profile and refuses to
start unless these values are supplied:

```text
FPM_DB_URL=jdbc:postgresql://host:5432/database
FPM_DB_USERNAME=...
FPM_DB_PASSWORD=...
FPM_JWT_KEY_PATH=/run/secrets/jwt-keypair.pem
FPM_LICENSE_KEY_ID=issuer-key-1
FPM_LICENSE_PUBLIC_KEY=<base64 Ed25519 public key>
FPM_ALLOWED_ORIGIN=https://app.example.test
```

The production profile maps the supplied license key ID and public key to
`app.licensing.trusted-keys`. Additional keys can be supplied with Spring's
relaxed `APP_LICENSING_TRUSTED_KEYS_<KID>` environment naming. Mount the RSA JWT key pair read-only; the
license issuer's Ed25519 private key never belongs on the server. Production
forces `app.security.refresh-cookie-secure=true`; the cookie is HttpOnly,
SameSite=Strict, scoped to `/api/v1/auth`, and has the configured refresh-token
lifetime.

Do not put passwords, private keys, signed license documents or real credentials
in the image or repository. Supply them through the runtime environment or a
secret mount. The trusted license values are public keys, not issuer private
keys.

## Health and lifecycle

- `/api/v1/system/liveness` is a public process liveness probe.
- `/api/v1/system/readiness` is the public database readiness probe and is the
  container/orchestration healthcheck.
- `/api/v1/system/health` remains the detailed public readiness response.

The endpoints return only status, database status and UTC timestamp; they do
not expose environment, credentials, paths or stack traces. Run setup once at
`/setup` (or `POST /api/v1/setup/initialize`), then log in and activate a
license issued by the external licensing process. Restarting preserves the
PostgreSQL named volume and mounted security keys.

Back up the SQLite file (including its WAL safely after stopping the service) or
use the PostgreSQL backup tooling for the database volume. Test restores before
relying on a backup.

## Release verification

```text
docker compose config
docker compose build
docker compose up -d
scripts/smoke.sh http://localhost:8080
```

The focused browser smoke flow is `cd apps/web && pnpm e2e` after installing
Playwright browsers. It is intentionally a small release check, not a full
functional test suite.
