# Agent Notes

- This repository does not keep a test suite.
- Prefer production-safe migrations that work on PostgreSQL.
- Avoid `CLOB`/`@Lob` mappings for long text. For shared H2/PostgreSQL schemas, prefer bounded `VARCHAR(32600)` fields and matching Liquibase `VARCHAR(32600)` migrations instead of `TEXT`/`LONGVARCHAR`.
- Use ProtonMail SMTP defaults outside the local profile; keep `application-local.properties` pointed at the fake SMTP server.
- Keep changes aligned with the existing Spring Boot and Angular structure.
- Use generated OpenAPI Angular API services and generated request/response models from `client/src/app/api` for backend calls; do not add custom/raw `HttpClient` calls for application API endpoints. If an endpoint or model is missing, update `server/src/main/resources/openapi.yaml` and regenerate the client instead of bypassing the generated API layer.
- Skip full builds for small, low-risk changes when they are not needed to verify the edit; reserve builds for code paths that actually need compilation or runtime confirmation.
