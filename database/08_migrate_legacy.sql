-- For the legacy COURSEWORK database only; BACK UP the database first.
-- Never apply 01_schema.sql over an already populated schema.
USE smart_home;
GO
IF COL_LENGTH('dbo.Device', 'on_state') IS NULL
    ALTER TABLE dbo.Device ADD on_state BIT NOT NULL
        CONSTRAINT DF_Device_on_state DEFAULT (0);
GO
IF COL_LENGTH('dbo.ConditionTable', 'sensor_id') IS NULL
    ALTER TABLE dbo.ConditionTable ADD sensor_id INT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Condition_Sensor')
    ALTER TABLE dbo.ConditionTable ADD CONSTRAINT FK_Condition_Sensor
        FOREIGN KEY (sensor_id) REFERENCES dbo.Sensor(sensor_id);
GO
-- Legacy conditions only stored the sensor TYPE, not its identity.
-- Backfill ONLY if there is exactly one matching sensor in the rule's home.
;WITH matches AS (
    SELECT c.condition_id, MIN(s.sensor_id) AS sensor_id, COUNT(*) AS matching_sensors
    FROM dbo.ConditionTable c
    JOIN dbo.Rules ru ON ru.rule_id = c.rule_id
    JOIN dbo.Room ro ON ro.home_id = ru.home_id
    JOIN dbo.Device d ON d.room_id = ro.room_id
    JOIN dbo.Sensor s ON s.device_id = d.device_id
        AND LOWER(REPLACE(s.sensor_type, ' ', '_')) = LOWER(c.attribute)
    WHERE c.sensor_id IS NULL
    GROUP BY c.condition_id
)
UPDATE c SET sensor_id = m.sensor_id
FROM dbo.ConditionTable c JOIN matches m ON m.condition_id = c.condition_id
WHERE m.matching_sensors = 1;
GO
-- ACTION REQUIRED: manually resolve remaining ambiguous/missing sensor IDs.
SELECT c.condition_id, c.rule_id, c.attribute
FROM dbo.ConditionTable c WHERE c.sensor_id IS NULL;
GO
