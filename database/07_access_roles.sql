-- Roles and grants only; intentionally NO hardcoded SQL login passwords.
-- Create a SQL Server login using your secure administrator tooling and
-- assign its database user to smart_home_app_role manually.
USE smart_home;
GO
IF DATABASE_PRINCIPAL_ID(N'smart_home_app_role') IS NULL
    CREATE ROLE smart_home_app_role;
GO
-- Explicitly allow read-only access to operational tables; do not expose [User].password_hash.
GRANT SELECT ON OBJECT::dbo.Home TO smart_home_app_role;
GRANT SELECT ON OBJECT::dbo.UserHome TO smart_home_app_role;
GRANT SELECT ON OBJECT::dbo.Room TO smart_home_app_role;
GRANT SELECT ON OBJECT::dbo.Device TO smart_home_app_role;
GRANT SELECT ON OBJECT::dbo.Sensor TO smart_home_app_role;
GRANT SELECT ON OBJECT::dbo.Rules TO smart_home_app_role;
GRANT SELECT ON OBJECT::dbo.ConditionTable TO smart_home_app_role;
GRANT SELECT ON OBJECT::dbo.ActionTable TO smart_home_app_role;
GRANT SELECT ON OBJECT::dbo.Event TO smart_home_app_role;
GRANT SELECT ON OBJECT::dbo.ExecutionLog TO smart_home_app_role;
GRANT INSERT ON OBJECT::dbo.Event TO smart_home_app_role;
GRANT INSERT ON OBJECT::dbo.ExecutionLog TO smart_home_app_role;
GRANT INSERT ON OBJECT::dbo.Rules TO smart_home_app_role;
GRANT INSERT ON OBJECT::dbo.ConditionTable TO smart_home_app_role;
GRANT INSERT ON OBJECT::dbo.ActionTable TO smart_home_app_role;
GRANT UPDATE(status) ON OBJECT::dbo.Rules TO smart_home_app_role;
GRANT UPDATE(on_state) ON OBJECT::dbo.Device TO smart_home_app_role;
GO
-- IMPORTANT: single-key demo API is NOT a substitute for authenticated per-user permissions.
