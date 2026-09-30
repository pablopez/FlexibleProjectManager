# ADR-0003: Authentication and Browser Token Storage

- Status: Accepted
- Date: 2026-09-26

## Context

Flexible Project Manager requires authentication for local multi-user deployments and future cloud deployments.

The browser client needs to authenticate API requests without storing long-lived credentials in JavaScript-accessible storage.

The authentication design must support:

- short-lived authenticated API sessions;
- logout and session revocation;
- page reloads without forcing the user to log in again;
- future authenticated streaming without changing the token model;
- local-first deployment today;
- future cloud deployment without redesigning the authentication model.

Storing long-lived tokens in `localStorage` or `sessionStorage` would expose them to JavaScript running in the page and therefore increases the impact of an XSS vulnerability.

A refresh token also needs server-side revocation and rotation so that sessions can be invalidated.

## Decision

Flexible Project Manager will use:

```text
Access token
    JWT
    short-lived
    stored only in frontend memory

Refresh token
    opaque random token
    long-lived
    stored in an HttpOnly cookie in the browser
    stored only as a hash in the database
```

The access token is sent to protected API endpoints using:

```http
Authorization: Bearer <access-token>
```

The refresh token is never intentionally exposed to frontend JavaScript.

## Access Token

The access token is a signed JWT.

For MVP 0.1:

```text
recommended lifetime: 15 minutes
```

The exact lifetime must be configurable.

The token should contain only claims required for authentication and authorization.

Typical claims:

```text
sub
userId
organizationId
memberId
roles
iat
exp
jti
```

Do not place sensitive personal or secret information in the JWT.

The JWT is an authentication credential, not a general-purpose user profile.

### Browser Storage

The access token is stored only in application memory.

It must NOT be stored in:

```text
localStorage
sessionStorage
IndexedDB
persistent browser caches
```

A full page reload therefore clears the access token.

The frontend restores the authenticated session by calling the refresh endpoint.

## Refresh Token

A refresh token is a cryptographically secure opaque random value.

It is not a JWT.

The token returned to the browser and the value stored in the database are different:

```text
browser:
    raw refresh token

database:
    cryptographic hash of refresh token
```

The raw refresh token must never be persisted in the database or logs.

The token must contain sufficient entropy to make guessing impractical.

## Refresh Token Cookie

The refresh token is delivered using an HTTP cookie.

Production cookie properties:

```text
HttpOnly
Secure
SameSite=Strict
Path=/api/v1/auth
```

The cookie must not be readable through JavaScript.

`Secure` may be relaxed only for local HTTP development where HTTPS is not available.

The cookie name should be application-specific and must not reveal the token value in logs.

## Login Flow

Conceptual flow:

```text
User enters email + password
        |
        v
POST /api/v1/auth/login
        |
        v
Backend verifies credentials
        |
        +--> creates short-lived access JWT
        |
        +--> creates opaque refresh token
        |        |
        |        +--> stores token hash in database
        |        +--> sends raw token as HttpOnly cookie
        |
        v
Response body returns access token
```

The frontend stores the access token in memory only.

## Session Restoration

After a page reload the frontend has no access token in memory.

It attempts session restoration using:

```text
POST /api/v1/auth/refresh
```

The browser sends the HttpOnly refresh cookie automatically.

If the refresh token is valid:

```text
backend
    rotates refresh token
    issues new access token
    returns new access token
    replaces refresh cookie
```

If it is invalid, expired or revoked, the frontend returns to the unauthenticated state.

## Refresh Token Rotation

Refresh tokens are rotated on every successful refresh.

Conceptual sequence:

```text
refresh token A
        |
        v
successful refresh
        |
        +--> revoke A
        +--> create token B
        +--> store hash(B)
        +--> send raw B cookie
```

A successfully used refresh token cannot be reused.

The database should maintain enough session information to support:

- token expiration;
- explicit revocation;
- logout;
- rotation;
- detection of reuse where practical.

## Refresh Token Persistence

The persistence model should include fields conceptually similar to:

```text
id
userId
tokenHash
createdAt
expiresAt
revokedAt
replacedByTokenId
```

Additional technical metadata may be added later if justified, but should not include unnecessary tracking data.

## Logout

Logout uses:

```text
POST /api/v1/auth/logout
```

The backend must:

```text
1. identify the current refresh session;
2. revoke the refresh token server-side;
3. expire/delete the refresh cookie.
```

The frontend must discard its in-memory access token.

Deleting the browser cookie alone is not sufficient because the server-side refresh session must also be revoked.

## Authorization

Protected HTTP endpoints use the access JWT.

The backend remains the source of truth for authorization.

Frontend role checks may improve the UI but must never be relied upon for security.

MVP roles remain:

```text
ADMIN
USER
VIEWER
```

Permissions are enforced server-side.

## Future streaming authentication

MVP 0.1 does not expose an event stream. If a future slice adds streaming, it
must use the short-lived bearer access token rather than the refresh cookie and
must receive a separate security review.

## CSRF and Cross-Origin Rules

Normal protected API requests authenticate using the Bearer access token and are not cookie-authenticated.

Only refresh/logout session endpoints rely on the refresh cookie.

The application should keep frontend and API same-site where practical.

The backend must use a restrictive CORS policy and must not use wildcard origins together with credentials.

Refresh and logout endpoints must use non-GET methods.

`SameSite=Strict` is part of the primary CSRF defense for the refresh cookie.

If a future deployment requires genuinely cross-site cookies, that change requires a separate security review and ADR update rather than silently weakening the cookie policy.

## XSS Considerations

This architecture reduces persistent token exposure but does not make XSS harmless.

Malicious JavaScript running in the application could still use an in-memory access token or make authenticated requests while the page is compromised.

The frontend should therefore continue to apply:

- safe React rendering practices;
- no unsafe HTML injection without explicit sanitization;
- restrictive Content Security Policy when deployment packaging is defined;
- dependency hygiene;
- no secrets embedded in frontend code.

## JWT Signing

Access JWTs must be cryptographically signed by the backend.

The signing key must not be committed to source control.

The exact key-management mechanism and signing algorithm will be selected during Slice 2 implementation based on the supported Spring Security stack.

The public API contract must not depend on a specific internal key-storage mechanism.

For local deployments, key material must be persisted outside packaged application resources so sessions remain valid across backend restarts where appropriate.

Future multi-instance/cloud deployments must use shared or centrally managed signing keys.

## Password Verification

Passwords are verified using the password hash created during system bootstrap/user creation.

The current password hashing algorithm is BCrypt.

Authentication code must use Spring Security password verification and must not compare plaintext passwords directly.

## Authentication Failure

Login failures must not disclose whether:

```text
the email exists
the account exists but is disabled
the password was incorrect
```

The public response should remain generic.

Detailed internal causes may be recorded through future audit functionality without exposing them to the client.

## Disabled Users

A user with status:

```text
DISABLED
```

must not be able to create a new authenticated session.

Existing refresh sessions for a disabled user must also be rejected.

Future user-disable functionality should revoke existing refresh sessions.

## Database

Refresh-session data belongs to the transactional Platform Core database.

For local deployments:

```text
SQLite
```

For future cloud deployments:

```text
PostgreSQL
```

Raw refresh tokens must never be persisted.

## Consequences

### Positive

- long-lived refresh credentials are unavailable to frontend JavaScript;
- access-token exposure is limited by short lifetime and memory-only storage;
- sessions survive page reloads through refresh;
- sessions can be revoked server-side;
- refresh-token reuse can be detected or restricted;
- future streaming can reuse Bearer authentication;
- architecture remains suitable for local and future cloud deployments.

### Negative

- the frontend must restore sessions after page reload;
- refresh-token rotation adds persistence and transaction logic;
- cookie behavior must be configured correctly for development and production;
- access-token renewal logic is more complex than storing a token in `localStorage`.

These costs are accepted for the improved security and session control.

## Out of Scope

This ADR does not define:

- OAuth2 social login;
- SSO;
- SAML;
- passkeys/WebAuthn;
- multi-factor authentication;
- password reset;
- invitations;
- email verification;
- external identity providers.

These features may be introduced later through separate architectural decisions.
