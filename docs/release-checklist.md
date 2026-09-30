# v0.1.0 release checklist

- [ ] Clean working tree and no secrets/private keys committed
- [ ] `mvn test` and `mvn package`
- [ ] PostgreSQL Testcontainers migration/startup test (Docker available)
- [ ] `pnpm test -- --run` and `pnpm build`
- [ ] OpenAPI lint/validation and bundled artifact review
- [ ] `docker compose config`
- [ ] Backend and frontend container builds
- [ ] Packaged application smoke test
- [ ] Focused browser release smoke flow
- [ ] SQLite and PostgreSQL migrations remain V1–V7 and checksum-valid
- [ ] Review health endpoints, cookie security and external secret mounts
- [ ] Create the `v0.1.0` tag manually after all checks pass
