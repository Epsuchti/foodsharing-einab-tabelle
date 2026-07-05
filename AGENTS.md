# Agent Notes

- This repository does not keep a test suite.
- Prefer production-safe migrations that work on PostgreSQL.
- Use ProtonMail SMTP defaults outside the local profile; keep `application-local.properties` pointed at the fake SMTP server.
- Keep changes aligned with the existing Spring Boot and Angular structure.
- Skip full builds for small, low-risk changes when they are not needed to verify the edit; reserve builds for code paths that actually need compilation or runtime confirmation.
