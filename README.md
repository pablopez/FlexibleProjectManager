# Flexible Project Manager

Flexible Project Manager is a modular project management platform designed to provide a reusable application core for future domain-specific modules.

The first milestone focuses exclusively on the platform itself: organizations, installations, licensing, authentication, users, roles, projects, settings, auditing and real-time application events.

Domain-specific functionality will be added later as independent modules.

## Goals

The platform is designed to be:

- Local-first
- Cloud-ready
- Modular
- Multi-user
- Extensible
- API-first
- Independent from specific business domains

## Initial Tech Stack

### Backend

- Java 21
- Spring Boot
- Spring Security
- JWT authentication
- JPA / Hibernate
- SQLite for local deployments
- PostgreSQL for future cloud deployments

### Frontend

- React
- TypeScript
- Vite

### API

The HTTP contract is defined using OpenAPI 3.1.

See:

`docs/api/openapi.yaml`

## Repository Structure

```text
flexible-project-manager/

├── apps/
│   ├── backend/
│   └── web/
│
├── docs/
│   ├── api/
│   │   └── openapi.yaml
│   │
│   ├── adr/
│   │   └── 0001-platform-core.md
│   │
│   ├── architecture.md
│   └── domain.md
│
├── AGENTS.md
└── README.md
```

## MVP 0.1

The first MVP will provide:

- Local installation initialization
- Single organization per local installation
- User authentication
- JWT access tokens
- Refresh tokens
- User management
- Basic roles and permissions
- Generic project management
- Project archive / restore
- License information and validation
- User and organization settings
- Audit log
- Server-Sent Events
- Initial React GUI

Projects are intentionally generic at this stage.

No domain-specific functionality belongs to the Platform Core.

## Out of Scope for MVP 0.1

The following technologies and features must not be introduced yet:

- Video processing
- FFmpeg
- C++
- Python
- AI / ML
- RabbitMQ
- Kubernetes
- External repositories
- Google Drive
- Dropbox
- S3
- Processing workers
- Domain-specific project logic

## Architecture

The Platform Core owns generic application capabilities.

Future business modules will depend on the Platform Core, but the Platform Core must never depend on business modules.

See:

- `docs/architecture.md`
- `docs/domain.md`
- `docs/adr/0001-platform-core.md`
- `docs/adr/0002-database-migrations.md`
- `docs/adr/0003-authentication.md`

## Status

Early architecture and MVP definition.
