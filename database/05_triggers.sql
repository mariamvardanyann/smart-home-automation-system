USE smart_home;
GO
CREATE TRIGGER trg_PreventActionOnInactiveDevice
ON ActionTable
AFTER INSERT
AS
BEGIN
    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN Device d ON i.device_id = d.device_id
        WHERE d.is_active = 0
    )
    BEGIN
        RAISERROR ('Cannot assign an action to an inactive device.', 16, 1);
        ROLLBACK TRANSACTION;
    END
END;
GO



CREATE TABLE AlertLog (
    alert_id INT IDENTITY(1,1) PRIMARY KEY,
    event_id INT NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    message VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_AlertLog_Event FOREIGN KEY (event_id)
        REFERENCES Event(event_id)
);
GO




CREATE TRIGGER trg_LogDangerousEvents
ON Event
AFTER INSERT
AS
BEGIN
    INSERT INTO AlertLog (event_id, alert_type, message)
    SELECT
        i.event_id,
        i.event_type,
        CASE
            WHEN i.event_type = 'Smoke' AND TRY_CAST(i.value AS FLOAT) > 90
                THEN 'Dangerous smoke level detected'
            WHEN i.event_type = 'Gas' AND TRY_CAST(i.value AS FLOAT) > 150
                THEN 'Dangerous gas level detected'
            WHEN i.event_type = 'Water Leak' AND i.value = '1'
                THEN 'Water leak detected'
        END
    FROM inserted i
    WHERE
        (i.event_type = 'Smoke' AND TRY_CAST(i.value AS FLOAT) > 90)
        OR (i.event_type = 'Gas' AND TRY_CAST(i.value AS FLOAT) > 150)
        OR (i.event_type = 'Water Leak' AND i.value = '1');
END;
GO
