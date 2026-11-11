# Windows development setup

## Prerequisites

- Microsoft SQL Server (Developer/Express) running locally, with TCP/IP enabled and a SQL login you control.
- SQL Server Management Studio (SSMS).
- JDK 17 or later (JDK 21 recommended), with `java` and `javac` visible in PowerShell.
- Internet access for Maven wrapper to download Maven/dependencies on first execution.

## Database

1. Back up any existing course database first.
2. For a **fresh** installation, open `database/00_create_database.sql` in SSMS (master connection) and execute.
3. Execute `01_schema.sql`, then `02_seed_demo.sql`, `03_views.sql`, `04_indexes.sql`, `05_triggers.sql`, `06_procedures.sql` in that order.
4. Optional: `07_access_roles.sql` defines a limited SQL database role. Provision a SQL login/password securely using SQL Server administrator tools; the project intentionally contains no account-creation script with hardcoded passwords.
5. For an **existing database**, use `08_migrate_legacy.sql` instead of `01_schema.sql`, and resolve every returned ambiguous condition before trying to load rules. Do not run demo seed blindly against production or student data.

## Backend environment (PowerShell)

Open PowerShell; replace the placeholders with your **local** secrets, not secrets written into source files:

```powershell
$env:SMART_HOME_DB_URL='jdbc:sqlserver://localhost:1433;databaseName=smart_home;encrypt=true;trustServerCertificate=true'
$env:SMART_HOME_DB_USER='YOUR_SQL_LOGIN'
$env:SMART_HOME_DB_PASSWORD='YOUR_LOCAL_PASSWORD'
$env:SMART_HOME_API_KEY='PASTE_A_UNIQUE_RANDOM_STRING_AT_LEAST_16_CHARACTERS'
$env:SMART_HOME_CORS_ORIGINS='http://localhost:5173'
cd backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The `trustServerCertificate=true` setting is acceptable for **local testing only**. Production requires proper server certificate verification and a real HTTPS deployment. The `.env.example` file is a reference, **not** automatically loaded by the Java application.

## Try the API

In a second PowerShell window, set the same key for that process:

```powershell
$headers = @{ 'X-API-Key' = $env:SMART_HOME_API_KEY }
Invoke-RestMethod -Uri 'http://localhost:8080/rules' -Headers $headers
```

Select a real `sensor_id` from SSMS before triggering a reading. See [API reference](API.md) and `examples/requests.http`.

## Troubleshooting

- **Missing SMART_HOME_API_KEY**: application intentionally refuses to start.
- **Login failed / database unavailable**: verify SQL Server service, TCP/IP port, JDBC URL, SQL user permissions, connection credentials and the database schema.
- **`Invalid column name 'sensor_id'` or `on_state`**: you have an old schema; back it up and apply the reviewed migration.
- **Maven download failure**: connect to the internet or use an existing offline Maven repository/cache. The `scripts/check-engine.ps1` smoke test needs only the JDK.
- **Rules cannot be loaded**: look for legacy ambiguous conditions that need exact `sensor_id` mapping or rules with no valid conditions/actions.
