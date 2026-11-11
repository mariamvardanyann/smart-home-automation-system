-- SQL Server / SSMS. Fresh database only; use migrations for an existing database.
USE smart_home;
GO

-- =========================
-- 1. USER TABLE
-- =========================
CREATE TABLE [User] (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT GETDATE()
);
GO

-- =========================
-- 2. HOME TABLE
-- =========================
CREATE TABLE Home (
    home_id INT IDENTITY(1,1) PRIMARY KEY,
    home_name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    timezone VARCHAR(50),
    created_at DATETIME DEFAULT GETDATE()
);
GO

-- =========================
-- 3. USERHOME TABLE
-- =========================
CREATE TABLE UserHome (
    user_home_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    home_id INT NOT NULL,
    access_role VARCHAR(50) NOT NULL,
    joined_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_UserHome_User FOREIGN KEY (user_id)
        REFERENCES [User](user_id)
        ON DELETE CASCADE,
    CONSTRAINT FK_UserHome_Home FOREIGN KEY (home_id)
        REFERENCES Home(home_id)
        ON DELETE CASCADE
);
GO

-- =========================
-- 4. ROOM TABLE
-- =========================
CREATE TABLE Room (
    room_id INT IDENTITY(1,1) PRIMARY KEY,
    home_id INT NOT NULL,
    room_name VARCHAR(100) NOT NULL,
    floor INT,
    CONSTRAINT FK_Room_Home FOREIGN KEY (home_id)
        REFERENCES Home(home_id)
        ON DELETE CASCADE
);
GO

-- =========================
-- 5. DEVICE TABLE
-- =========================
CREATE TABLE Device (
    device_id INT IDENTITY(1,1) PRIMARY KEY,
    room_id INT NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    device_type VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(100),
    installation_date DATE,
    is_active BIT NOT NULL DEFAULT 1, -- availability, NOT the power switch
    on_state BIT NOT NULL DEFAULT 0, -- simulated power state
    CONSTRAINT FK_Device_Room FOREIGN KEY (room_id)
        REFERENCES Room(room_id)
        ON DELETE CASCADE
);
GO

-- =========================
-- 6. SENSOR TABLE
-- =========================
CREATE TABLE Sensor (
    sensor_id INT IDENTITY(1,1) PRIMARY KEY,
    device_id INT NOT NULL,
    sensor_type VARCHAR(50) NOT NULL,
    unit VARCHAR(20),
    last_seen DATETIME,
    CONSTRAINT FK_Sensor_Device FOREIGN KEY (device_id)
        REFERENCES Device(device_id)
        ON DELETE CASCADE
);
GO

-- =========================
-- 7. EVENT TABLE
-- =========================
CREATE TABLE Event (
    event_id INT IDENTITY(1,1) PRIMARY KEY,
    sensor_id INT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    value VARCHAR(50) NOT NULL,
    [timestamp] DATETIME DEFAULT GETDATE(),
    source VARCHAR(50),
    CONSTRAINT FK_Event_Sensor FOREIGN KEY (sensor_id)
        REFERENCES Sensor(sensor_id)
        ON DELETE CASCADE
);
GO

-- =========================
-- 8. RULE TABLE
-- =========================
CREATE TABLE Rules (
    rule_id INT IDENTITY(1,1) PRIMARY KEY,
    home_id INT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    priority INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'active'
        CHECK (status IN ('active', 'inactive')),
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Rules_Home FOREIGN KEY (home_id)
        REFERENCES Home(home_id)
        ON DELETE CASCADE
);
GO

-- =========================
-- 9. CONDITION TABLE
-- =========================
CREATE TABLE ConditionTable (
    condition_id INT IDENTITY(1,1) PRIMARY KEY,
    rule_id INT NOT NULL,
    attribute VARCHAR(50) NOT NULL, -- legacy human-readable sensor type
    sensor_id INT NULL, -- exact sensor; nullable for migration of earlier coursework rows
    operator VARCHAR(10) NOT NULL,
    value VARCHAR(50) NOT NULL,
    logical_operator VARCHAR(10),
    group_id INT,
    CONSTRAINT FK_Condition_Rule FOREIGN KEY (rule_id)
        REFERENCES Rules(rule_id)
        ON DELETE CASCADE,
    CONSTRAINT FK_Condition_Sensor FOREIGN KEY (sensor_id)
        REFERENCES Sensor(sensor_id)
);
GO

-- =========================
-- 10. ACTION TABLE
-- =========================

CREATE TABLE ActionTable (
    action_id INT IDENTITY(1,1) PRIMARY KEY,
    rule_id INT NOT NULL,
    device_id INT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    parameters VARCHAR(255),
    CONSTRAINT FK_Action_Rule FOREIGN KEY (rule_id)
        REFERENCES Rules(rule_id)
        ON DELETE NO ACTION,
    CONSTRAINT FK_Action_Device FOREIGN KEY (device_id)
        REFERENCES Device(device_id)
        ON DELETE NO ACTION
);
GO


-- =========================
-- 11. EXECUTION LOG TABLE
-- =========================
CREATE TABLE ExecutionLog (
    log_id INT IDENTITY(1,1) PRIMARY KEY,
    rule_id INT NOT NULL,
    event_id INT NOT NULL,
    action_id INT NOT NULL,
    execution_time DATETIME DEFAULT GETDATE(),
    status VARCHAR(50) NOT NULL,
    error_message VARCHAR(255),
    CONSTRAINT FK_ExecutionLog_Rule FOREIGN KEY (rule_id)
        REFERENCES Rules(rule_id)
        ON DELETE NO ACTION,
    CONSTRAINT FK_ExecutionLog_Event FOREIGN KEY (event_id)
        REFERENCES Event(event_id)
        ON DELETE NO ACTION,
    CONSTRAINT FK_ExecutionLog_Action FOREIGN KEY (action_id)
        REFERENCES ActionTable(action_id)
        ON DELETE NO ACTION
);
GO


