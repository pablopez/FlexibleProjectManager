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

A license belongs to an organization and may optionally be restricted to an installation.

Main attributes:

```text
id
organizationId
installationId
licenseKey
type
status
issuedAt
expiresAt
signature
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
ACTIVE
EXPIRED
REVOKED
INVALID
```

A license may provide features and limits.

Example features:

```text
projects
future-module
```

Example limits:

```text
maxUsers
maxInstallations
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

## UserSetting

Represents a user preference.

Initial supported settings:

```text
language
theme
```

Initial values:

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

Represents an immutable record of a relevant action.

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

Examples:

```text
LOGIN_SUCCESS
LOGIN_FAILED

USER_CREATED
USER_UPDATED
USER_DISABLED

PROJECT_CREATED
PROJECT_UPDATED
PROJECT_ARCHIVED
PROJECT_RESTORED

LICENSE_ACTIVATED
LICENSE_DEACTIVATED
```

Audit metadata should remain bounded and structured. It must not be used as an unrestricted storage mechanism for arbitrary application data.

## ApplicationEvent

Represents something that happened inside the application.

Application events may be consumed by internal components and exposed to connected clients.

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

Server-to-client event delivery for MVP 0.1 will use Server-Sent Events.

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
