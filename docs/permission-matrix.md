# Effective system-role matrix

This is derived from the permission seed in the setup migration and is covered
by the setup integration test. Settings are authenticated self-service and do
not use a dedicated permission.

| Role | Users | Projects | Organization | License | Audit |
| --- | --- | --- | --- | --- | --- |
| ADMIN | read, create, update | read, create, update, archive | read, update | read, manage | read |
| USER | — | read, create, update | read | — | — |
| VIEWER | — | read | read | read | — |
