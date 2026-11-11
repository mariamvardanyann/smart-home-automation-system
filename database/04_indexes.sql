USE smart_home;
GO
CREATE INDEX IX_Room_HomeId ON Room(home_id);
GO

CREATE INDEX IX_Device_RoomId ON Device(room_id);
GO

CREATE INDEX IX_Sensor_DeviceId ON Sensor(device_id);
GO

CREATE INDEX IX_Event_SensorId ON Event(sensor_id);
GO

CREATE INDEX IX_Event_Timestamp ON Event([timestamp]);
GO

CREATE INDEX IX_Rules_HomeId ON Rules(home_id);
GO

CREATE INDEX IX_ActionTable_RuleId ON ActionTable(rule_id);
GO

CREATE INDEX IX_ExecutionLog_RuleId ON ExecutionLog(rule_id);
GO

-- Support exact sensor lookups, rule loading and per-home activity queries.
CREATE INDEX IX_ConditionTable_RuleId ON ConditionTable(rule_id);
GO
CREATE INDEX IX_ConditionTable_SensorId ON ConditionTable(sensor_id);
GO
CREATE INDEX IX_ActionTable_DeviceId ON ActionTable(device_id);
GO
CREATE INDEX IX_ExecutionLog_EventId ON ExecutionLog(event_id);
GO
CREATE UNIQUE INDEX UX_UserHome_User_Home ON UserHome(user_id, home_id);
GO
