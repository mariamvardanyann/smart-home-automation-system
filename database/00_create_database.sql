-- Run this file once in SSMS against master (new database only).
IF DB_ID(N'smart_home') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE smart_home');
END;
GO
