-- Small, deterministic, idempotent demo data; no real accounts or credentials.
USE smart_home;
GO
SET NOCOUNT ON;
IF NOT EXISTS (SELECT 1 FROM Home WHERE home_name = 'Demo Smart Home')
BEGIN
    BEGIN TRANSACTION;
    BEGIN TRY
        INSERT INTO [User](name, email, password_hash, role)
        VALUES ('Demo User', 'demo@example.invalid', 'NO_LOGIN_DEMO_ONLY', 'user');
        DECLARE @UserId INT = SCOPE_IDENTITY();
        INSERT INTO Home(home_name, address, timezone)
        VALUES ('Demo Smart Home', 'Example address (fictional)', 'Asia/Yerevan');
        DECLARE @HomeId INT = SCOPE_IDENTITY();
        INSERT INTO UserHome(user_id, home_id, access_role) VALUES (@UserId, @HomeId, 'owner');
        INSERT INTO Room(home_id, room_name, floor) VALUES (@HomeId, 'Hallway', 1);
        DECLARE @RoomId INT = SCOPE_IDENTITY();

        INSERT INTO Device(room_id, device_name, device_type, is_active, on_state)
        VALUES (@RoomId, 'Demo Motion Detector', 'Motion Sensor', 1, 0);
        DECLARE @MotionDevice INT = SCOPE_IDENTITY();
        INSERT INTO Device(room_id, device_name, device_type, is_active, on_state)
        VALUES (@RoomId, 'Demo Light', 'Smart Lamp', 1, 0);
        DECLARE @LightDevice INT = SCOPE_IDENTITY();
        INSERT INTO Device(room_id, device_name, device_type, is_active, on_state)
        VALUES (@RoomId, 'Demo Thermostat', 'Thermostat', 1, 0);
        DECLARE @TemperatureDevice INT = SCOPE_IDENTITY();
        INSERT INTO Device(room_id, device_name, device_type, is_active, on_state)
        VALUES (@RoomId, 'Demo Heater', 'Heater Controller', 1, 0);
        DECLARE @HeaterDevice INT = SCOPE_IDENTITY();

        INSERT INTO Sensor(device_id, sensor_type, unit)
        VALUES (@MotionDevice, 'Motion', 'boolean');
        DECLARE @MotionSensor INT = SCOPE_IDENTITY();
        INSERT INTO Sensor(device_id, sensor_type, unit)
        VALUES (@TemperatureDevice, 'Temperature', 'Celsius');
        DECLARE @TemperatureSensor INT = SCOPE_IDENTITY();

        INSERT INTO Rules(home_id, rule_name, description, priority)
        VALUES (@HomeId, 'Hallway Motion Lighting', 'Turn on demo light when motion is detected', 1);
        DECLARE @MotionRule INT = SCOPE_IDENTITY();
        INSERT INTO ConditionTable(rule_id, sensor_id, attribute, operator, value)
        VALUES (@MotionRule, @MotionSensor, 'motion', '==', '1');
        INSERT INTO ActionTable(rule_id, device_id, action_type)
        VALUES (@MotionRule, @LightDevice, 'turn_on');

        INSERT INTO Rules(home_id, rule_name, description, priority)
        VALUES (@HomeId, 'Heating Comfort', 'Turn on demo heater below 18 degrees Celsius', 2);
        DECLARE @HeatRule INT = SCOPE_IDENTITY();
        INSERT INTO ConditionTable(rule_id, sensor_id, attribute, operator, value)
        VALUES (@HeatRule, @TemperatureSensor, 'temperature', '<', '18');
        INSERT INTO ActionTable(rule_id, device_id, action_type)
        VALUES (@HeatRule, @HeaterDevice, 'turn_on');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
-- Inspect generated IDs; do not assume identity values across different databases.
SELECT sensor_id, sensor_type FROM Sensor ORDER BY sensor_id;
SELECT device_id, device_name, on_state, is_active FROM Device ORDER BY device_id;
GO
