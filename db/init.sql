-- Idempotent: creates one database, login and user per service.
IF DB_ID('sitedb') IS NULL CREATE DATABASE sitedb;
GO
IF DB_ID('equipmentdb') IS NULL CREATE DATABASE equipmentdb;
GO
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'site_user')
    CREATE LOGIN site_user WITH PASSWORD = '$(SITE_DB_PASSWORD)', CHECK_POLICY = OFF;
GO
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'equipment_user')
    CREATE LOGIN equipment_user WITH PASSWORD = '$(EQUIPMENT_DB_PASSWORD)', CHECK_POLICY = OFF;
GO
USE sitedb;
IF USER_ID('site_user') IS NULL CREATE USER site_user FOR LOGIN site_user;
ALTER ROLE db_owner ADD MEMBER site_user;
GO
USE equipmentdb;
IF USER_ID('equipment_user') IS NULL CREATE USER equipment_user FOR LOGIN equipment_user;
ALTER ROLE db_owner ADD MEMBER equipment_user;
GO
