-- Optional analytical examples, run after setup.
USE smart_home;
GO
SELECT * FROM [User];
GO

SELECT * FROM Home;
GO


--Shows which users have access to which homes
SELECT u.name, h.home_name, uh.access_role
FROM UserHome uh
JOIN [User] u ON uh.user_id = u.user_id
JOIN Home h ON uh.home_id = h.home_id
ORDER BY h.home_id;
GO



SELECT h.home_name, r.room_name, r.floor
FROM Room r
JOIN Home h ON r.home_id = h.home_id
ORDER BY h.home_id, r.floor;
GO


SELECT d.device_name, d.device_type, r.room_name, h.home_name
FROM Device d
JOIN Room r ON d.room_id = r.room_id
JOIN Home h ON r.home_id = h.home_id
ORDER BY h.home_id;
GO



SELECT s.sensor_id, s.sensor_type, s.unit, d.device_name, d.device_type
FROM Sensor s
JOIN Device d ON s.device_id = d.device_id
ORDER BY s.sensor_id;
GO


SELECT * FROM Event
ORDER BY [timestamp] DESC;
GO


SELECT * FROM Event
WHERE 
    (event_type = 'Smoke' AND TRY_CAST(value AS INT) > 90)
    OR (event_type = 'Gas' AND TRY_CAST(value AS INT) > 150)
    OR (event_type = 'Water Leak' AND value = '1');
GO



SELECT * FROM Rules
WHERE status = 'active';
GO


SELECT r.rule_name, c.attribute, c.operator, c.value
FROM Rules r
JOIN ConditionTable c ON r.rule_id = c.rule_id
ORDER BY r.rule_id;
GO


SELECT r.rule_name, a.action_type, a.parameters
FROM Rules r
JOIN ActionTable a ON r.rule_id = a.rule_id
ORDER BY r.rule_id;
GO


SELECT el.log_id, r.rule_name, el.execution_time, el.status, el.error_message
FROM ExecutionLog el
JOIN Rules r ON el.rule_id = r.rule_id
ORDER BY el.execution_time DESC;
GO


SELECT el.log_id, r.rule_name, el.execution_time, el.error_message
FROM ExecutionLog el
JOIN Rules r ON el.rule_id = r.rule_id
WHERE el.status = 'failed';
GO


SELECT h.home_name, COUNT(d.device_id) AS device_count
FROM Home h
JOIN Room r ON h.home_id = r.home_id
JOIN Device d ON r.room_id = d.room_id
GROUP BY h.home_name
ORDER BY device_count DESC;
GO


SELECT sensor_type, COUNT(*) AS total_sensors
FROM Sensor
GROUP BY sensor_type
ORDER BY total_sensors DESC;
GO


SELECT h.home_name, COUNT(r.rule_id) AS total_rules
FROM Home h
JOIN Rules r ON h.home_id = r.home_id
GROUP BY h.home_name
ORDER BY total_rules DESC;
GO



SELECT h.home_name, COUNT(uh.user_id) AS total_users
FROM Home h
JOIN UserHome uh ON h.home_id = uh.home_id
GROUP BY h.home_name
HAVING COUNT(uh.user_id) > 1
ORDER BY total_users DESC;
GO



SELECT sensor_id, MAX([timestamp]) AS latest_event
FROM Event
GROUP BY sensor_id
ORDER BY latest_event DESC;
GO


SELECT * FROM Device
WHERE is_active = 0;
GO



SELECT 
    e.event_type,
    e.value,
    r.rule_name,
    a.action_type,
    el.status
FROM ExecutionLog el
JOIN Event e ON el.event_id = e.event_id
JOIN Rules r ON el.rule_id = r.rule_id
JOIN ActionTable a ON el.action_id = a.action_id
ORDER BY el.execution_time DESC;
GO