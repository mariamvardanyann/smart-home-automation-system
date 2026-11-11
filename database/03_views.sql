USE smart_home;
GO
CREATE VIEW vw_UserHomeAccess AS
SELECT 
    u.user_id,
    u.name AS user_name,
    h.home_id,
    h.home_name,
    uh.access_role,
    uh.joined_at
FROM UserHome uh
JOIN [User] u ON uh.user_id = u.user_id
JOIN Home h ON uh.home_id = h.home_id;
GO


CREATE VIEW vw_HomeDevices AS
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
FROM Device d
JOIN Room r ON d.room_id = r.room_id
JOIN Home h ON r.home_id = h.home_id;
GO



CREATE VIEW vw_ActiveRules AS
SELECT
    rule_id,
    home_id,
    rule_name,
    description,
    priority,
    status,
    created_at
FROM Rules
WHERE status = 'active';
GO



CREATE VIEW vw_DangerousEvents AS
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
    OR (event_type = 'Water Leak' AND value = '1');
GO





CREATE VIEW vw_ExecutionSummary AS
SELECT
    el.log_id,
    r.rule_name,
    a.action_type,
    e.event_type,
    e.value,
    el.execution_time,
    el.status,
    el.error_message
FROM ExecutionLog el
JOIN Rules r ON el.rule_id = r.rule_id
JOIN ActionTable a ON el.action_id = a.action_id
JOIN Event e ON el.event_id = e.event_id;
GO




CREATE VIEW vw_SensorDeviceInfo AS
SELECT
    s.sensor_id,
    s.sensor_type,
    s.unit,
    s.last_seen,
    d.device_id,
    d.device_name,
    d.device_type
FROM Sensor s
JOIN Device d ON s.device_id = d.device_id;
GO




CREATE VIEW vw_FailedExecutions AS
SELECT
    el.log_id,
    r.rule_name,
    a.action_type,
    el.execution_time,
    el.error_message
FROM ExecutionLog el
JOIN Rules r ON el.rule_id = r.rule_id
JOIN ActionTable a ON el.action_id = a.action_id
WHERE el.status = 'failed';
GO