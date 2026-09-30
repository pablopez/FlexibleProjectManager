# Platform Domain

## 1. Purpose

This document defines the initial domain vocabulary for the Flexible Project Manager Platform Core.

Only generic application concepts belong here.

Business-domain concepts must be defined in their corresponding modules.

## Organization

Represents the organization using the platform.

Local deployments contain exactly one organization.

A future cloud deployment may contain multiple organizations.

Main attributes:

```text
id
name
slug
status
createdAt
updatedAt
```

Status:

```text
ACTIVE
DISABLED
```

## Installation

Represents a concrete installation of the application.

An installation belongs to an organization.

In the local deployment model, setup creates exactly one installation associated
with the single organization. The installation identity is its server-controlled
immutable UUID; management operations cannot create, transfer, or replace it.

Examples:

```text
Windows workstation
Linux workstation
On-premise server
Cloud deployment
```

Main attributes:

```text
id
organizationId
name
platform
applicationVersion
status
createdAt
lastSeenAt
```

Status:

```text
ACTIVE
DISABLED
UNLICENSED
```

## License

Represents authorization to use the product.

A license is a signed authorization artifact bound to exactly one installation.
The installation is identified by the immutable server-controlled
`Installation.id`. Organization identity is derived through the current
installation relationship and is not an independently signed license claim.

Main attributes:

```text
id
organizationId
installationId
signedLicense
type
issuedAt
expiresAt
maxUsers
licenseFeatures
```

Types:

```text
TRIAL
SUBSCRIPTION
PERPETUAL
DEVELOPMENT
```

Status:

```text
UNLICENSED
ACTIVE
EXPIRED
INVALID
```

A valid signed license always contains a non-null `maxUsers` value. Effective
entitlements expose opaque `licenseFeatures` keywords only while the license is
ACTIVE.

Example licenseFeatures:

```text
module.video-qc
video-qc.hdr
video-qc.imf
```

Example limit:

```text
maxUsers
```

## User

Represents a person capable of authenticating with the platform.

Main attributes:

```text
id
email
passwordHash
displayName
status
createdAt
updatedAt
```

Status:

```text
ACTIVE
DISABLED
```

Passwords must never be stored directly.

## OrganizationMember

Represents membership of a user in an organization.

Main attributes:

```text
id
userId
organizationId
status
joinedAt
```

Status:

```text
ACTIVE
DISABLED
```

This association allows the domain to support multiple organizations in future cloud deployments.

An organization must always retain at least one effective active administrator. An effective active administrator is an
ACTIVE user with an ACTIVE organization membership and the ADMIN role. Disabling the last such administrator or removing
ADMIN from the last such administrator is rejected.

## Role

Represents a named collection of permissions.

Initial roles:

```text
ADMIN
USER
VIEWER
```

Roles are system-defined in MVP 0.1.

They cannot be created, renamed or deleted by users in this version.

## Permission

Represents an operation a user may perform.

Examples:

```text
users:read
users:create
users:update

projects:read
projects:create
projects:update
projects:archive

organization:read
organization:update

license:read
license:manage

audit:read
```

For MVP 0.1, `audit:read` is assigned to `ADMIN` only. `USER` and `VIEWER` do
not receive this permission. Audit recording is internal application behavior
and does not use user-facing `audit:create`, `audit:update` or `audit:delete`
permissions; those permissions do not exist.

Future modules may register additional permissions.

## RefreshToken

Represents a renewable authenticated session.

Main attributes:

```text
id
userId
tokenHash
createdAt
expiresAt
revokedAt
```

Refresh tokens must be stored as hashes.

## Project

Represents a generic project within the platform.

Project is deliberately domain-agnostic.

Main attributes:

```text
id
organizationId
name
description
status
createdBy
createdAt
updatedAt
```

Status:

```text
ACTIVE
ARCHIVED
```

For MVP 0.1 a project contains no domain-specific functionality.

## UserPreferences

Represents preferences owned by the authenticated user. Preferences are not
organization-, installation-, project-, or module-owned.

The preference row is optional. Absence of a row means the effective defaults
are English and the light theme. A row is created only after the first
successful update and is deleted with its owning user.

Initial supported settings:

```text
language
theme
```

Supported values:

```text
language:
- es
- en

theme:
- light
- dark
- system
```

Additional user settings may be added later through explicit versioned contracts.

## OrganizationSetting

Represents configuration shared by an organization.

Module-specific configuration must not be mixed with Platform Core configuration without an explicit namespace.

For MVP 0.1, organization settings must use explicitly supported keys rather than unrestricted arbitrary values.

## AuditEntry

Represents an immutable, append-only record of a relevant Platform Core state
change. Audit history is scoped to an organization and is read through
`GET /api/v1/audit`. The organization scope is derived from
`CurrentActor.organizationId`; clients cannot select an organization ID.

Audit entries are created internally only after a successful mutation. There is
no public audit write API, and normal application behavior provides no update or
delete operation.

Main attributes:

```text
id
organizationId
actorUserId
action
resourceType
resourceId
metadata
createdAt
```

`actorUserId` may be nullable for possible future system events, but every Slice
9 event has an actor derived from `CurrentActor.userId()`. `resourceId` is
nullable. Current mutations record stable UUID references where available:

```text
PROJECT_*             -> project ID
USER_*                -> target user ID
ORGANIZATION_UPDATED  -> organization ID
INSTALLATION_UPDATED  -> installation ID
USER_PREFERENCES_UPDATED -> actor user ID
LICENSE_*             -> verified license ID when available
```

An invalid or unverifiable license artifact must never supply a resource ID;
license deactivation may therefore have a null `resourceId`.

Actions and resource types are stable, uppercase machine-readable strings. They
use a format comparable to `^[A-Z][A-Z0-9_]*$`, with a reasonable bounded
length, but are intentionally extensible strings rather than closed global
enums. They are never localized in persistence or API responses.

Initial Slice 9 actions:

```text
PROJECT_CREATED
PROJECT_UPDATED
PROJECT_ARCHIVED
PROJECT_RESTORED

USER_CREATED
USER_UPDATED
USER_DISABLED
USER_REACTIVATED
USER_ROLES_CHANGED

ORGANIZATION_UPDATED
INSTALLATION_UPDATED

LICENSE_ACTIVATED
LICENSE_REPLACED
LICENSE_DEACTIVATED

USER_PREFERENCES_UPDATED
```

Initial resource types:

```text
PROJECT
USER
ORGANIZATION
INSTALLATION
LICENSE
USER_PREFERENCES
```

Authentication events are explicitly deferred from Slice 9:

```text
LOGIN_SUCCESS
LOGIN_FAILED
LOGOUT
REFRESH_SUCCESS
refresh-token events
```

Audit metadata is optional and must be explicitly constructed, minimal,
non-secret, non-authoritative and bounded. Slice 9 supports only controlled
metadata such as:

```json
{
  "changedFields": ["displayName", "status"]
}
```

`changedFields` contains unique field names. Commands, request DTOs, HTTP
request bodies and entities must never be serialized into audit metadata.

Audit metadata must never contain:

```text
passwords
password hashes
JWT access tokens
refresh tokens
refresh-token hashes
cookies
Authorization headers
full signed license JWS values
private keys
signing key material
cryptographic secrets
```

Complete emails, display names, project descriptions and organization names
should also not be copied into metadata. Prefer IDs and changed field names
over value snapshots.

The REST response may contain the actor's `id` and current `displayName` for
presentation. The display name is not an authoritative historical snapshot and
must not require persisting user payloads in the audit record.

Audit persistence retains raw UUID identifiers as historical references. It
must not require cascading foreign keys to users, projects, installations or
organizations that could delete historical audit entries when resources change
or are removed. No retention policy exists in MVP; audit events are retained
indefinitely.

Audit recording participates in the same transaction as the originating
business mutation:

```text
business mutation succeeds
+ audit append succeeds
    -> commit

audit append fails
    -> business mutation rolls back
```

Audit reads require `audit:read` and remain subject to normal licensing
enforcement. `GET /api/v1/audit` is not a licensing recovery endpoint, so an
inactive license produces `403 LICENSE_NOT_ACTIVE`. Internal audit recording
does not perform a separate licensing check because the originating business
operation has already passed central licensing enforcement.

Examples:

```text
PROJECT_CREATED
USER_CREATED
```

Audit history is intended to provide timestamp, actor, action, resource type,
resource ID and controlled metadata to later frontend views. The backend stores
machine action codes only; localization belongs to the frontend.

## ApplicationEvent

Represents something that happened inside the application.

Application events may be introduced by a future slice; MVP 0.1 does not expose
an event stream.

Conceptual structure:

```text
id
type
timestamp
organizationId
resourceType
resourceId
payload
```

Application events are not necessarily persistent.

Initial examples:

```text
PROJECT_CREATED
PROJECT_UPDATED
PROJECT_ARCHIVED
PROJECT_RESTORED

USER_CREATED
USER_UPDATED

LICENSE_ACTIVATED
LICENSE_DEACTIVATED
```

Server-to-client event delivery is deferred beyond MVP 0.1.

Persistent user notifications are outside the scope of MVP 0.1.

## System Bootstrap

The local application requires a one-time initialization process before normal authentication can be used.

A fresh installation starts in an uninitialized state.

The bootstrap process is responsible for creating the minimum Platform Core state required to operate the application.

### Initialization State

The system is considered initialized when the following resources exist:

- Organization
- Installation
- Initial administrator user
- Administrator organization membership
- Initial system roles and permissions

License activation is NOT required to complete system initialization.

A newly initialized installation may remain in an `UNLICENSED` state until a valid license is activated.

### First Run Flow

```text
Application starts
        |
        v
Check initialization state
        |
        +---- initialized ----> Login
        |
        +---- not initialized
                  |
                  v
             Setup screen
                  |
                  v
        Organization information
        Administrator information
        Installation name
                  |
                  v
             Initialize
                  |
                  v
              Login
```

### Bootstrap Input

The initialization process requires:

```text
organization
    name

installation
    name

administrator
    email
    displayName
    password
```

The client must NOT provide:

```text
organizationId
installationId
userId
roles
permissions
platform
applicationVersion
```

These values are controlled by the backend.

### Bootstrap Behavior

Initialization must execute atomically.

Either all initial resources are created successfully or none of them are persisted.

The backend must:

```text
1. Verify that the system is not already initialized.
2. Create the Organization.
3. Generate the Installation identity.
4. Detect the current platform.
5. Create the initial system roles.
6. Create the initial permissions.
7. Assign permissions to the system roles.
8. Create the administrator User.
9. Create the OrganizationMember relationship.
10. Assign the ADMIN role to the administrator.
11. Persist the complete initialization state.
```

The administrator password must be hashed before persistence.

The installation `id` is a backend-generated UUID and is the canonical identity of the installation.

When another resource refers to an installation, such as a license, this value is exposed as `installationId`.

The installation identifier is not a secret and must not be derived from hardware identifiers.

License security must rely on cryptographic signature validation rather than on obscuring the installation identifier.

### System Roles

The initial roles created during bootstrap are:

```text
ADMIN
USER
VIEWER
```

These roles are system-defined in MVP 0.1.

They cannot be created, renamed or deleted by users.

### Initialization Security

Bootstrap endpoints are unauthenticated because no user exists before initialization.

However, initialization is available only while the system is uninitialized.

After successful initialization, any additional initialization attempt must be rejected.

Expected behavior:

```text
POST /setup/initialize

uninitialized
    -> initialization succeeds

initialized
    -> SYSTEM_ALREADY_INITIALIZED
```

The initialization operation must be protected against concurrent initialization attempts.

Only one initialization transaction may succeed.

### Local Deployment Constraint

MVP 0.1 local deployments support exactly one Organization per installation.

This is a deployment constraint and must not be encoded as a limitation of the general domain model.

Future cloud deployments may use a different provisioning process and support multiple organizations.

### License State After Bootstrap

Bootstrap does not automatically create a commercial license.

After initialization:

```text
Installation.status = UNLICENSED
```

The administrator can authenticate and access the license-management area.

Features requiring a valid entitlement may remain unavailable until a valid license is activated.

Development environments may provide a separate development-only mechanism for bypassing or supplying licensing, but this behavior must not be part of the production bootstrap contract.

## Relationships

```text
Organization
│
├── Installation
├── License
├── OrganizationMember
│       │
│       └── User
│
├── Project
├── OrganizationSetting
└── AuditEntry

User
│
├── OrganizationMember
├── RefreshToken
├── UserSetting
└── AuditEntry
```

## Domain Boundary

The Platform Core may know:

```text
organizations
users
projects
licenses
settings
events
```

The Platform Core must NOT know what a project is used for.

Future modules own domain-specific project functionality.
