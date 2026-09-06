# Production Bootstrap

How to stand up a fresh Skultem environment: infra, secrets, first deploy, first
school, and the first `SYSTEM_ADMIN` account. Do these in order — the system admin
bootstrap endpoint depends on a school already existing, and depends on the
bootstrap token env var being set only for that one call.

## 1. Infrastructure

`docker-compose.yml` at the repo root is a **local dev stack only** — it hardcodes
credentials (`moriba@2024`, `password123`) and exposes Postgres/Redis/MinIO
without TLS. Do not run it in production. Provision instead:

- **Postgres 15+** — managed instance, own credentials, network-restricted to the
  backend.
- **Redis 7+** — managed instance, own password, network-restricted.
- **Cloudflare R2** (or S3-compatible bucket) — this is what the app actually
  writes to in production via `R2StorageService`; MinIO in the compose file is
  only a local stand-in for it, not a second thing to deploy.

## 2. Required environment variables

`application.yaml` ships with working *development* fallbacks for every secret
(DB password, JWT signing key, mail API token, R2 keys) so the app runs
out-of-the-box locally. **Every one of these must be overridden in production** —
none of the checked-in defaults are safe to run with:

| Variable | Purpose | Notes |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASS` | Postgres connection | Point at the managed instance from step 1 |
| `SKULTEM_SECRET_KEY` | JWT signing secret | Generate a fresh random value (e.g. `openssl rand -base64 32`); rotating it invalidates all active sessions |
| `SKULTEM_ACCESS_EXP_MS` / `SKULTEM_REFRESH_EXP_MS` | Token lifetimes | Defaults (15 min / 7 days) are usually fine, override only if policy requires otherwise |
| `R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET_NAME`, `R2_PUBLIC_URL` | Object storage | Use your own R2 bucket/keys, not the checked-in sample account |
| `MAIL_MODE` | Mail sending mode | Set to the production mode (not `dev`) so mail actually sends |
| `EMAIL_API_KEY` | Mailtrap/email provider token | Use your own account's token |
| `PARENT_ASSIGN_TEMPLATE`, `PARENT_WELCOME_TEMPLATE`, `TEACHER_WELCOME_TEMPLATE`, `USER_ASSIGN_TEMPLATE`, `USER_WELCOME_TEMPLATE`, `WELCOME_TEMPLATE` | Email template IDs | Only if using different templates than the shared dev ones |
| `SYSTEM_ADMIN_BOOTSTRAP_TOKEN` | Gates `POST /api/v1/system/bootstrap` | See step 4 — set only long enough to run the bootstrap call, then unset/rotate |
| `PORT` | HTTP port | Defaults to `8080` |

Also update `SecurityConfig.corsConfigurationSource()` if the production frontend
domain isn't already covered by `https://*.skultem.space`.

## 3. First deploy

Deploy the backend with the env vars above set. Flyway runs automatically on
startup (`spring.flyway.enabled: true`) and applies `db/migration` against the
configured database — no manual migration step needed. Confirm the app is up via
`GET /actuator/health`.

## 4. Create the first school

`POST /api/v1/school` is unauthenticated by design (`SecurityConfig` permits all
of `/api/v1/school/**`) since there's no admin yet to gate it. Create the
school that will anchor the system admin's account (see `docs/school.http` for
the full payload shape):

```
POST /api/v1/school
Content-Type: application/json

{
  "name": "...",
  "domain": "your-domain",
  "email": "...",
  "givenNames": "...",
  "familyName": "...",
  "password": "...",
  ...
}
```

Bootstrap (next step) doesn't need this `domain` value — it just needs *a*
school to exist, any school, to anchor the new `SchoolUser` row to (a DB
constraint, not a real scoping — see step 5).

## 5. Bootstrap the system admin

`BootstrapSystemAdminUseCase` is the only path onto `Role.SYSTEM_ADMIN`. It's
reachable pre-auth but double-gated: a caller-supplied token checked against
`system.admin.bootstrap-token`, and a check that no `SYSTEM_ADMIN` exists yet
anywhere in the system. Left unset, the endpoint refuses every request — it
fails closed, not open.

1. Set `SYSTEM_ADMIN_BOOTSTRAP_TOKEN` to a freshly generated random value and
   (re)deploy/restart the backend so it picks it up.
2. Call the endpoint once:

   ```
   POST /api/v1/system/bootstrap
   Content-Type: application/json

   {
     "token": "<SYSTEM_ADMIN_BOOTSTRAP_TOKEN value>",
     "email": "admin@your-domain",
     "password": "...",
     "givenNames": "...",
     "familyName": "..."
   }
   ```

   `domain` is optional — omit it (as above) and the new `SYSTEM_ADMIN` is
   anchored onto whichever school exists; `SYSTEM_ADMIN` isn't scoped to that
   school once granted (see `PermissionService.isSystemAdmin()`), so it doesn't
   matter which one. Pass `domain` only to anchor onto a specific school on
   purpose.

   If `email` matches an existing user (e.g. the school owner created in step 4
   promoting themselves), that account is reused and only granted the
   `SYSTEM_ADMIN` role — its existing password is untouched.
3. **Immediately unset or rotate `SYSTEM_ADMIN_BOOTSTRAP_TOKEN`** and
   redeploy/restart. The endpoint also self-disables on its own — once any
   `SYSTEM_ADMIN` exists, it refuses further calls regardless of the token — but
   don't rely on that alone; clearing the token removes the credential from the
   running environment entirely.
4. Confirm: log in as the new admin and call `GET /api/v1/system/stats`
   (`@permissionService.isSystemAdmin()`-gated) to verify the role took.

From here on, additional `SYSTEM_ADMIN` accounts are created the ordinary way —
an existing system admin using the normal user-creation flow — not through this
endpoint again.

## 6. Frontend

Build and serve the Nuxt app (`app/`) per Nuxt's standard production build
(`npm run build`, then serve `.output/`); point it at the deployed backend's
public URL and make sure that URL is one of the CORS-allowed origins from step 2.
