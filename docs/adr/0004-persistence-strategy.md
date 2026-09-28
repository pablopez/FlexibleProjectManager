# ADR-0004: Persistence Strategy for Platform Core

- Status: Accepted
- Date: 2026-09-28

## Context

Flexible Project Manager uses a modular monolith architecture with:

- SQLite for local deployments;
- PostgreSQL as a future cloud deployment database;
- Flyway as the source of truth for relational schema migrations;
- explicit application/domain boundaries;
- infrastructure adapters for persistence.

The first implementation slices introduced some persistence using `JdbcTemplate`:

- first-run bootstrap;
- authentication identity lookup;
- refresh-token persistence and rotation.

Those cases contain technical or concurrency-sensitive operations where explicit SQL is useful.

The next slices introduce normal Platform Core business data such as Projects, Users, Organization settings and other domain entities.

Using only low-level JDBC for all business persistence would increase mapping boilerplate and couple infrastructure code unnecessarily to SQL and `ResultSet` handling.

Using JPA for every persistence concern would also be inappropriate for some technical operations where conditional SQL updates and explicit transactional behavior are clearer.

## Decision

Flexible Project Manager will use a mixed persistence strategy with a clear responsibility boundary.

### Business Persistence

Normal Platform Core business aggregates and entities should use JPA/Hibernate through infrastructure adapters.

Examples include:

```text
Project
User management
Organization management
User settings
future generic Platform Core entities
```

Application/domain code must not depend directly on Spring Data JPA or Hibernate.

The preferred dependency direction is:

```text
domain / application
        |
        v
repository port
        |
        v
infrastructure adapter
        |
        v
Spring Data JPA / Hibernate
        |
        v
SQLite or PostgreSQL
```

API controllers must never expose JPA entities directly.

### Technical / Specialized Persistence

`JdbcTemplate` or explicit SQL remains acceptable when the persistence operation is primarily technical, concurrency-sensitive, or benefits from exact SQL semantics.

Current examples:

```text
system bootstrap guard
refresh-token rotation
authentication technical/session persistence
specialized atomic conditional updates
```

Using JDBC for these cases does not require converting them to JPA merely for consistency.

Future explicit SQL must remain isolated in infrastructure.

## Domain and JPA Model Separation

For substantial business modules, domain models and JPA persistence models should be separate when doing so protects architectural boundaries.

Example:

```text
projects/
├── domain/
│   └── Project.java
├── application/
│   ├── ProjectService.java
│   └── ProjectRepository.java
├── infrastructure/
│   ├── ProjectJpaEntity.java
│   ├── SpringDataProjectRepository.java
│   └── JpaProjectRepositoryAdapter.java
└── api/
    ├── ProjectController.java
    └── ProjectDtos.java
```

The domain object must not require JPA annotations.

The JPA entity is an infrastructure concern.

Mapping between domain and persistence models must remain explicit and small.

For very small modules, unnecessary duplication may be avoided when the architecture remains clear, but API models must still remain separate from persistence models.

## Schema Ownership

Flyway remains the source of truth for the physical database schema.

JPA/Hibernate must not create or mutate the production schema.

Production configuration must not use:

```text
ddl-auto=create
ddl-auto=create-drop
ddl-auto=update
```

JPA mappings must conform to Flyway migrations.

Schema validation may be enabled where practical.

## Database Portability

Business persistence must remain compatible with SQLite and PostgreSQL.

JPA does not eliminate database differences.

Rules:

1. Use portable relational mappings where practical.
2. Avoid PostgreSQL-only features in Platform Core business persistence unless isolated and explicitly justified.
3. Use UUID domain identifiers.
4. Use UTC timestamps represented as `Instant` in application/domain code.
5. Keep database-specific SQL inside infrastructure.
6. Keep SQLite and PostgreSQL Flyway migrations semantically aligned.
7. Test important persistence behavior against real SQLite.
8. Add PostgreSQL integration coverage as cloud/runtime support becomes active.

## Transactions

Transaction boundaries belong at the application/use-case level when a use case modifies multiple persistence operations atomically.

Infrastructure repositories should not define business workflow transaction boundaries.

Examples:

```text
Create Project
Archive Project
Update User
Activate License
```

may each define an application transaction where required.

Specialized persistence operations may additionally use database-level conditional updates or constraints to guarantee concurrency safety.

## Queries

Simple aggregate retrieval may use Spring Data repository methods.

More complex read queries may use:

- explicit JPQL;
- projections;
- dedicated read adapters;
- `JdbcTemplate` when explicit SQL is materially clearer.

The application layer should depend on use-case-oriented repository/query ports rather than on Spring Data interfaces.

## Projects as First JPA Business Module

The generic Project module will be the first business module implemented using this strategy.

Its persistence will use:

```text
Project domain model
        ↓
ProjectRepository port
        ↓
JPA infrastructure adapter
        ↓
Spring Data JPA
```

Project remains a generic Platform Core concept.

It must contain no Video QC or future-module-specific fields.

## Existing JDBC Code

This ADR does not require rewriting existing working Slice 1 or Slice 2 persistence.

Existing bootstrap and authentication JDBC code remains valid.

Refactoring existing technical persistence to JPA should only happen if a concrete architectural or maintenance benefit appears.

## Consequences

### Positive

- normal business persistence avoids repetitive low-level JDBC mapping;
- domain/application code remains independent of Hibernate;
- explicit SQL remains available where it provides clearer concurrency semantics;
- persistence choices are made by responsibility rather than by ideology;
- SQLite and PostgreSQL compatibility remain visible;
- Projects establish a repeatable pattern for later Platform Core modules.

### Negative

- the codebase uses more than one persistence style;
- domain/JPA separation introduces some mapping code;
- developers must understand when JPA or explicit SQL is appropriate.

These costs are accepted because using the same persistence tool for every problem would create worse coupling or unnecessary complexity.

## Alternatives Considered

### JdbcTemplate for all persistence

Rejected as the default for business entities.

It provides excellent SQL control but would create unnecessary mapping and query boilerplate for ordinary Platform Core CRUD-style persistence.

### JPA for all persistence

Rejected.

Some technical and concurrency-sensitive operations are clearer and safer using explicit conditional SQL.

### JPA entities as the domain model

Not selected as the default for substantial modules.

It reduces mapping code but couples domain objects to persistence concerns and makes module boundaries less explicit.

## Out of Scope

This ADR does not define:

- caching;
- CQRS;
- event sourcing;
- read replicas;
- database sharding;
- multi-tenant database isolation;
- Video QC analysis data persistence;
- object storage.

Those concerns require separate decisions if they become necessary.
