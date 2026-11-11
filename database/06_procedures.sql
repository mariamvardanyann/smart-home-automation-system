USE smart_home;
GO
CREATE PROCEDURE sp_GetDevicesByHome
    @HomeId INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT 
        h.home_id,
        h.home_name,
        r.room_id,
        r.room_name,
        d.device_id,
        d.device_name,
        d.device_type,
        d.manufacturer,
        d.is_active
    FROM Home h
    JOIN Room r ON h.home_id = r.home_id
    JOIN Device d ON r.room_id = d.room_id
    WHERE h.home_id = @HomeId
    ORDER BY r.room_name, d.device_name;
END;
GO



CREATE PROCEDURE sp_GetDangerousEvents
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        event_id,
        sensor_id,
        event_type,
        value,
        [timestamp],
        source
    FROM Event
    WHERE
        (event_type = 'Smoke' AND TRY_CAST(value AS FLOAT) > 90)
        OR (event_type = 'Gas' AND TRY_CAST(value AS FLOAT) > 150)
        OR (event_type = 'Water Leak' AND value = '1')
    ORDER BY [timestamp] DESC;
END;
GO






CREATE PROCEDURE sp_UpdateRuleStatus
    @RuleId INT,
    @NewStatus VARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;

    IF @NewStatus NOT IN ('active', 'inactive')
    BEGIN
        RAISERROR('Status must be either active or inactive.', 16, 1);
        RETURN;
    END

    UPDATE Rules
    SET status = @NewStatus
    WHERE rule_id = @RuleId;

    SELECT rule_id, rule_name, status
    FROM Rules
    WHERE rule_id = @RuleId;
END;
GO









CREATE PROCEDURE sp_InsertEvent
    @SensorId INT,
    @EventType VARCHAR(50),
    @Value VARCHAR(50),
    @Source VARCHAR(50)
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (
        SELECT 1
        FROM Sensor
        WHERE sensor_id = @SensorId
    )
    BEGIN
        RAISERROR('Invalid sensor_id. Sensor does not exist.', 16, 1);
        RETURN;
    END

    INSERT INTO Event (sensor_id, event_type, value, [timestamp], source)
    VALUES (@SensorId, @EventType, @Value, GETDATE(), @Source);

    SELECT TOP 1 *
    FROM Event
    WHERE sensor_id = @SensorId
    ORDER BY event_id DESC;
END;
GO








CREATE PROCEDURE sp_GetExecutionHistoryByRule
    @RuleId INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        el.log_id,
        r.rule_name,
        e.event_type,
        e.value,
        a.action_type,
        el.execution_time,
        el.status,
        el.error_message
    FROM ExecutionLog el
    JOIN Rules r ON el.rule_id = r.rule_id
    JOIN Event e ON el.event_id = e.event_id
    JOIN ActionTable a ON el.action_id = a.action_id
    WHERE el.rule_id = @RuleId
    ORDER BY el.execution_time DESC;
END;
GO








CREATE PROCEDURE sp_GetUsersByHome
    @HomeId INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        h.home_id,
        h.home_name,
        u.user_id,
        u.name,
        u.email,
        uh.access_role
    FROM UserHome uh
    JOIN [User] u ON uh.user_id = u.user_id
    JOIN Home h ON uh.home_id = h.home_id
    WHERE h.home_id = @HomeId
    ORDER BY uh.access_role, u.name;
END;
GO