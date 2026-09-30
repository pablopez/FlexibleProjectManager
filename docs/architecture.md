# Architecture

## 1. Overview

Flexible Project Manager is a modular application platform.

The first version is a generic project management application.

The architecture must allow future business modules to extend project functionality without coupling the Platform Core to those modules.

The initial architecture follows a modular monolith approach.

```text
React Web Application
        |
        | REST
        v
Spring Boot Platform Core
        |
        +---- Authentication
        +---- Organizations
        +---- Installations
        +---- Licensing
        +---- Users
        +---- Authorization
        +---- Projects
        +---- Settings
        +---- Audit
        |
        v
Persistence
        |
        +---- SQLite      Local
        |
        +---- PostgreSQL  Future Cloud
```

## 2. Architecture Style

The initial backend will be a modular monolith.

Microservices are explicitly not required for the initial platform.

Modules should have clear responsibilities and controlled dependencies.

The architecture should make later extraction possible without designing for distributed systems prematurely.

## 3. Platform Core

The Platform Core contains functionality that is reusable across different business domains.

Initial modules:

```text
platform

├── authentication
├── users
├── organization
├── installation
├── licensing
├── authorization
├── projects
├── settings
├── audit
```

Future domain-specific modules will live outside the Platform Core.

Example:

```text
modules/

└── future-business-module/
```

The Platform Core must not depend on modules.

Modules may depend on Platform Core contracts.

## 4. Backend Layers

Each sufficiently complex module may use the following separation:

```text
domain
application
infrastructure
api
```

### Domain

Contains business concepts and rules.

It should avoid dependencies on HTTP, databases and presentation concerns.

### Application

Contains use cases.

Examples:

```text
CreateProject
ArchiveProject
CreateUser
ActivateLicense
```

Application services coordinate domain objects and infrastructure ports.

### Infrastructure

Contains technical implementations.

Examples:

```text
JPA repositories
SQLite
PostgreSQL
JWT implementation
cryptography
filesystem access
```

### API

Contains HTTP controllers and DTOs.

Controllers translate HTTP requests into application use cases.

Controllers must not contain business logic.

## 5. Frontend

The frontend is a React + TypeScript application.

Initial structure:

```text
src/

├── app/
│   ├── router/
│   ├── layout/
│   └── providers/
│
├── entities/
│   ├── user/
│   ├── organization/
│   └── project/
│
├── features/
│   ├── auth/
│   ├── projects/
│   ├── users/
│   └── settings/
│
├── pages/
│   ├── login/
│   ├── dashboard/
│   ├── projects/
│   ├── project/
│   ├── users/
│   ├── organization/
│   ├── license/
│   └── settings/
│
└── shared/
    ├── api/
    ├── ui/
    └── lib/
```

The frontend communicates only with the Spring Boot API.

It never accesses persistence directly.

## 6. Authentication

Authentication uses:

```text
email + password
        |
        v
Spring Security
        |
        +---- short-lived JWT access token
        |
        +---- revocable refresh token
```

Access tokens identify the current user.

Refresh tokens are persisted as hashes and can be revoked.

## 7. Authorization

Authorization is based on roles and permissions.

Initial roles:

```text
ADMIN
USER
VIEWER
```

Permissions should remain more granular internally even if the initial GUI only exposes roles.

## 8. Organization Model

Local installations support exactly one organization.

This is a deployment restriction, not a domain restriction.

The model should remain compatible with a future cloud deployment containing multiple organizations.

## 9. Projects

Project is intentionally generic.

Initial Project responsibilities:

```text
identity
name
description
status
creator
creation time
update time
```

Project must not contain domain-specific fields.

Future modules may associate their own domain objects with a project.

## 10. Persistence

### Local

SQLite.

Used for:

```text
organizations
installations
users
memberships
roles
permissions
licenses
projects
settings
refresh tokens
audit
```

### Cloud

PostgreSQL may replace SQLite for the Platform Core.

The domain and API contracts must remain independent from the database implementation.

## 11. Application Events

Application-event streaming is deferred beyond MVP 0.1. The current release
exposes the REST API and persistent audit log only.

## 12. API

The API is defined by:

`docs/api/openapi.yaml`

HTTP API prefix:

```text
/api/v1
```

The OpenAPI contract must remain independent from JPA entities.

## 13. Deployment Profiles

### Desktop / Local

```text
React
Spring Boot
SQLite
```

Single organization.

Multiple users are allowed.

### Future Cloud

```text
Load Balancer
      |
Spring Boot instances
      |
PostgreSQL
```

Multiple organizations.

Horizontal scaling may be introduced later.

## 14. Non-Goals

MVP 0.1 must not introduce:

```text
microservices
Kubernetes
distributed queues
media processing
AI
external repositories
processing workers
```

These capabilities belong to later milestones.
