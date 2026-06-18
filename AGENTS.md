# Agent Notes

- This repository does not keep a test suite.
- Prefer production-safe migrations that work on PostgreSQL.
- Keep schema changes in the initial Liquibase migration for now; do not add follow-up migrations unless explicitly needed.
- Keep changes aligned with the existing Spring Boot and Angular structure.
