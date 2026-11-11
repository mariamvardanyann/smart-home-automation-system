# Security policy and prototype status

**Do not deploy this service to the public internet.** This is a university smart-home database simulator, not a production IoT security system.

## What changed

- Database connection URL, username and password are **required environment variables**; none are committed to the repository.
- All non-health HTTP routes require the `X-API-Key` header. Startup fails if a 16+ character demo key is not configured.
- CORS origins must be explicitly configured; cross-origin wildcard access has been removed.
- Database CRUD is performed through parameterized JDBC statements.
- Existing hardcoded example SQL login passwords were excluded from publishable scripts.

## Important remaining gaps

A **single shared API key is not user authentication or per-home authorization**. If two people know the key, both can access the same API endpoints; the backend currently has no authenticated user identity or tenant-level access checks. This should only be run locally or in a private controlled test environment. Do not embed the shared API key in a publicly hosted browser client.

For a real deployment, implement Spring Security with user identities, protected password hashing, home ownership checks on each request, rate limits, HTTPS with trusted certificates, secure secret storage, access logs, and IoT adapter security. Treat demo user records as fictitious, not usable login credentials.

## Before publication

1. **Rotate any passwords** that appeared in the original archives or PDF, especially the database login credentials and demonstration SQL logins. Deleting the original file from the new repository does not make previously-used passwords safe.
2. Scan all files and Git history for credentials; never commit local `.env` files, real database dumps or personal identifiers.
3. Obtain both original team members' approval for credits, licensing and publication.
4. Keep the service off public networks until real per-user authorization exists.
