package com.smarthome.smart_home_backend.db;

import com.smarthome.smart_home_backend.ds.model.*;
import com.smarthome.smart_home_backend.dto.CreateRuleRequest;
import com.smarthome.smart_home_backend.dto.HomeDashboardResponse;
import com.smarthome.smart_home_backend.model.Device;
import com.smarthome.smart_home_backend.model.Home;
import com.smarthome.smart_home_backend.model.Room;
import com.smarthome.smart_home_backend.dto.ReadableLogResponse;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseLoader {
    private record RuleRow(int id, int homeId, String name, String status) { }

    public static List<Rule> loadRules() {
        List<Rule> rules = new ArrayList<>();
        String sql = "SELECT rule_id, home_id, rule_name, status FROM Rules";
        try (Connection conn = DatabaseConfig.openConnection()) {
            // Consume/close the result set before nested condition/action queries.
            // SQL Server drivers do not necessarily enable Multiple Active Result Sets (MARS).
            List<RuleRow> rows = new ArrayList<>();
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    rows.add(new RuleRow(rs.getInt("rule_id"), rs.getInt("home_id"),
                            rs.getString("rule_name"), rs.getString("status")));
                }
            }
            for (RuleRow row : rows) {
                Rule rule = new Rule(row.id(), row.name());
                if ("inactive".equalsIgnoreCase(row.status())) rule.disable();
                for (Condition condition : loadConditions(conn, row.id(), row.homeId())) rule.addCondition(condition);
                for (Action action : loadActions(conn, row.id())) {
                    verifyDeviceBelongsToHome(conn, action.getDeviceId(), row.homeId());
                    rule.addAction(action);
                }
                rules.add(rule);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load automation rules", e);
        }
        return rules;
    }

    private record ConditionRow(Integer sensorId, String attribute, String operator, String value) { }

    private static List<Condition> loadConditions(Connection conn, int ruleId, int homeId) throws SQLException {
        List<ConditionRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT sensor_id, attribute, operator, value FROM ConditionTable WHERE rule_id = ?")) {
            ps.setInt(1, ruleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("sensor_id");
                    rows.add(new ConditionRow(rs.wasNull() ? null : id, rs.getString("attribute"),
                            rs.getString("operator"), rs.getString("value")));
                }
            }
        }
        // Avoid issuing nested statements while the SQL Server result set is open (MARS off).
        List<Condition> conditions = new ArrayList<>();
        for (ConditionRow row : rows) {
            int sensorId = row.sensorId() == null
                    ? resolveSensorIdByAttribute(conn, homeId, row.attribute()) : row.sensorId();
            if (sensorId <= 0) {
                throw new IllegalStateException("Rule " + ruleId + " in home " + homeId
                        + " has a missing or ambiguous sensor for '" + row.attribute()
                        + "'. Review database/08_migrate_legacy.sql before startup.");
            }
            sensorTypeForHome(conn, sensorId, homeId);
            double threshold;
            try { threshold = Double.parseDouble(row.value()); }
            catch (NumberFormatException e) { throw new IllegalStateException("Invalid threshold in rule " + ruleId, e); }
            conditions.add(new Condition(sensorId,
                    ComparisonOperator.fromSymbol(normalizeOperator(row.operator())), threshold));
        }
        return conditions;
    }

    private static List<Action> loadActions(Connection conn, int ruleId) throws SQLException {
        List<Action> actions = new ArrayList<>();

        String actionQuery = """
                SELECT device_id, action_type, parameters
                FROM ActionTable
                WHERE rule_id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(actionQuery)) {
            ps.setInt(1, ruleId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int deviceId = rs.getInt("device_id");
                    String actionType = rs.getString("action_type");
                    String parameters = rs.getString("parameters");

                    String command;
                    if (parameters != null && !parameters.isBlank()) {
                        command = actionType + " [" + parameters + "]";
                    } else {
                        command = actionType;
                    }

                    Action action = new Action(deviceId, command);
                    actions.add(action);
                }
            }
        }

        return actions;
    }

    private static int resolveSensorIdByAttribute(Connection conn, int homeId, String attribute) throws SQLException {
        String sensorType = mapAttributeToSensorType(attribute);

        if (sensorType == null) {
            return -1;
        }

        String sensorQuery = """
                SELECT TOP 2 s.sensor_id
                FROM Sensor s
                JOIN Device d ON s.device_id = d.device_id
                JOIN Room r ON d.room_id = r.room_id
                WHERE r.home_id = ?
                  AND s.sensor_type = ?
                ORDER BY s.sensor_id
                """;

        try (PreparedStatement ps = conn.prepareStatement(sensorQuery)) {
            ps.setInt(1, homeId);
            ps.setString(2, sensorType);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int matchedId = rs.getInt("sensor_id");
                    return rs.next() ? -1 : matchedId;
                }
            }
        }

        return -1;
    }

    private static String mapAttributeToSensorType(String attribute) {
        if (attribute == null) {
            return null;
        }

        return switch (attribute.toLowerCase()) {
            case "smoke" -> "Smoke";
            case "gas" -> "Gas";
            case "temperature" -> "Temperature";
            case "motion" -> "Motion";
            case "water_leak" -> "Water Leak";
            case "power_consumption" -> "Power Consumption";
            default -> null;
        };
    }

    private static String normalizeOperator(String operator) {
        if (operator == null) {
            throw new IllegalArgumentException("Operator cannot be null.");
        }

        operator = operator.trim();

        return switch (operator) {
            case "=" -> "==";
            case "<>" -> "!=";
            default -> operator;
        };
    }

    public static List<Device> loadDevices() {
        List<Device> devices = new ArrayList<>();

        String query = """
            SELECT 
                d.device_id,
                d.device_name,
                d.device_type,
                d.is_active,
                d.on_state,
                r.room_name
            FROM Device d
            JOIN Room r ON d.room_id = r.room_id
        """;

        try (Connection conn = DatabaseConfig.openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Device device = new Device(
                        rs.getInt("device_id"),
                        rs.getString("device_name"),
                        rs.getString("room_name"),
                        rs.getString("device_type"),
                        rs.getBoolean("on_state"),
                        rs.getBoolean("is_active")
                );

                devices.add(device);
            }

        } catch (Exception e) {
            throw new IllegalStateException("Cannot load devices", e);
        }

        return devices;
    }

    public static List<Home> loadHomesByUser(int userId) {
        List<Home> homes = new ArrayList<>();

        String query = """
        SELECT h.home_id, h.home_name, h.address, h.timezone, uh.access_role
        FROM Home h
        JOIN UserHome uh ON h.home_id = uh.home_id
        WHERE uh.user_id = ?
    """;

        try (Connection conn = DatabaseConfig.openConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                homes.add(new Home(
                        rs.getInt("home_id"),
                        rs.getString("home_name"),
                        rs.getString("address"),
                        rs.getString("timezone"),
                        rs.getString("access_role")
                ));
            }

        } catch (Exception e) {
            throw new IllegalStateException("Database operation failed", e);
        }

        return homes;
    }

    public static List<Room> loadRoomsByHome(int homeId) {
        List<Room> rooms = new ArrayList<>();

        String query = """
        SELECT room_id, home_id, room_name, floor
        FROM Room
        WHERE home_id = ?
    """;

        try (Connection conn = DatabaseConfig.openConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, homeId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                rooms.add(new Room(
                        rs.getInt("room_id"),
                        rs.getInt("home_id"),
                        rs.getString("room_name"),
                        rs.getInt("floor")
                ));
            }

        } catch (Exception e) {
            throw new IllegalStateException("Database operation failed", e);
        }

        return rooms;
    }


    public static List<Device> loadDevicesByRoom(int roomId) {
        List<Device> devices = new ArrayList<>();

        String query = """
        SELECT d.device_id, d.device_name, d.device_type, d.is_active, d.on_state, r.room_name
        FROM Device d
        JOIN Room r ON d.room_id = r.room_id
        WHERE d.room_id = ?
    """;

        try (Connection conn = DatabaseConfig.openConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                devices.add(new Device(
                        rs.getInt("device_id"),
                        rs.getString("device_name"),
                        rs.getString("room_name"),
                        rs.getString("device_type"),
                        rs.getBoolean("on_state"),
                        rs.getBoolean("is_active")
                ));
            }

        } catch (Exception e) {
            throw new IllegalStateException("Database operation failed", e);
        }

        return devices;
    }


    /** Stores a new rule atomically, resolving exact sensor IDs and validating home ownership. */
    public static int saveRule(CreateRuleRequest request) {
        if (request == null || request.name == null || request.name.isBlank()
                || request.name.length() > 100 || request.homeId <= 0
                || request.conditions == null || request.conditions.isEmpty()
                || request.actions == null || request.actions.isEmpty()) {
            throw new IllegalArgumentException("Rule must include name, home, conditions and actions");
        }
        String insertRule = "INSERT INTO Rules (home_id, rule_name, status) VALUES (?, ?, 'active')";
        String insertCondition = "INSERT INTO ConditionTable (rule_id, sensor_id, attribute, operator, value) VALUES (?, ?, ?, ?, ?)";
        String insertAction = "INSERT INTO ActionTable (rule_id, device_id, action_type, parameters) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.openConnection()) {
            conn.setAutoCommit(false);
            try {
                int ruleId;
                try (PreparedStatement ps = conn.prepareStatement(insertRule, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, request.homeId);
                    ps.setString(2, request.name.trim());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No generated rule ID returned");
                        ruleId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(insertCondition)) {
                    for (var c : request.conditions) {
                        if (c == null || c.sensorId <= 0 || !Double.isFinite(c.value))
                            throw new IllegalArgumentException("Invalid condition sensor or threshold");
                        String normalizedOp = ComparisonOperator.fromSymbol(c.operator).getSymbol();
                        String sensorType = sensorTypeForHome(conn, c.sensorId, request.homeId);
                        ps.setInt(1, ruleId);
                        ps.setInt(2, c.sensorId);
                        ps.setString(3, sensorType.toLowerCase(java.util.Locale.ROOT).replace(' ', '_'));
                        ps.setString(4, normalizedOp);
                        ps.setString(5, Double.toString(c.value));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = conn.prepareStatement(insertAction)) {
                    for (var a : request.actions) {
                        if (a == null || a.deviceId <= 0 || a.command == null || a.command.isBlank()
                                || a.command.length() > 50) {
                            throw new IllegalArgumentException("Invalid device action");
                        }
                        validateDeviceHome(conn, a.deviceId, request.homeId);
                        ps.setInt(1, ruleId);
                        ps.setInt(2, a.deviceId);
                        ps.setString(3, a.command.trim());
                        ps.setNull(4, Types.VARCHAR);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                return ruleId;
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not save automation rule", ex);
        }
    }

    private static String sensorTypeForHome(Connection conn, int sensorId, int homeId) throws SQLException {
        String sql = """
                SELECT s.sensor_type FROM Sensor s
                JOIN Device d ON d.device_id = s.device_id
                JOIN Room r ON d.room_id = r.room_id
                WHERE s.sensor_id = ? AND r.home_id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sensorId); ps.setInt(2, homeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Sensor does not belong to requested home: " + sensorId);
                return rs.getString(1);
            }
        }
    }

    private static void verifyDeviceBelongsToHome(Connection conn, int deviceId, int homeId) throws SQLException {
        String query = """
                SELECT 1 FROM Device d JOIN Room r ON d.room_id = r.room_id
                WHERE d.device_id = ? AND r.home_id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, deviceId); ps.setInt(2, homeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Rule action targets a different home or unknown device");
            }
        }
    }

    private static void validateDeviceHome(Connection conn, int deviceId, int homeId) throws SQLException {
        String sql = """
                SELECT 1 FROM Device d JOIN Room r ON d.room_id = r.room_id
                WHERE d.device_id = ? AND r.home_id = ? AND d.is_active = 1
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceId); ps.setInt(2, homeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Active device does not belong to requested home: " + deviceId);
            }
        }
    }

    public static void updateDeviceState(int deviceId, boolean isActive) {
        String query = """
        UPDATE Device
        SET on_state = ?
        WHERE device_id = ? AND is_active = 1
    """;

        try (Connection conn = DatabaseConfig.openConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setBoolean(1, isActive);
            ps.setInt(2, deviceId);

            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("Device unavailable or not found: " + deviceId);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update device state", e);
        }
    }


    public static int saveEvent(int sensorId, double value) {
        String eventType = getEventTypeBySensor(sensorId);

        String query = """
        INSERT INTO Event (sensor_id, event_type, value, timestamp, source)
        VALUES (?, ?, ?, ?, ?)
    """;

        try (Connection conn = DatabaseConfig.openConnection();
             PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, sensorId);
            ps.setString(2, eventType);
            ps.setDouble(3, value);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.setString(5, "backend");

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Could not persist sensor event", e);
        }

        throw new IllegalStateException("No event ID generated");
    }

    private static String getEventTypeBySensor(int sensorId) {
        String query = """
        SELECT sensor_type
        FROM Sensor
        WHERE sensor_id = ?
    """;

        try (Connection conn = DatabaseConfig.openConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, sensorId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("sensor_type");
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("Database operation failed", e);
        }

        throw new IllegalArgumentException("Unknown sensor ID: " + sensorId);
    }


    /** Changes simulated device state and records its execution in one DB transaction. */
    public static void applyDeviceActionAndLog(ExecutionRecord record, Boolean onState) {
        if (record.getEventId() <= 0) throw new IllegalArgumentException("Missing persisted event ID");
        String update = "UPDATE Device SET on_state = ? WHERE device_id = ? AND is_active = 1";
        String insert = """
                INSERT INTO ExecutionLog (rule_id, event_id, action_id, execution_time, status, error_message)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.openConnection()) {
            conn.setAutoCommit(false);
            try {
                int actionId = findActionId(conn, record.getRuleId(), record.getDeviceId(), record.getActionCommand());
                if (actionId < 0) throw new IllegalArgumentException("Stored action not found");
                if (onState != null) {
                    try (PreparedStatement ps = conn.prepareStatement(update)) {
                        ps.setBoolean(1, onState);
                        ps.setInt(2, record.getDeviceId());
                        if (ps.executeUpdate() != 1)
                            throw new IllegalArgumentException("Inactive or missing target device");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setInt(1, record.getRuleId());
                    ps.setInt(2, record.getEventId());
                    ps.setInt(3, actionId);
                    ps.setTimestamp(4, new Timestamp(record.getTimestamp()));
                    ps.setString(5, record.getStatus());
                    ps.setString(6, record.getErrorMessage());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | RuntimeException error) {
                conn.rollback();
                throw error;
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Could not apply action and save execution log", error);
        }
    }

    private static int findActionId(Connection conn, int ruleId, int deviceId, String command) throws SQLException {
        String query = """
        SELECT TOP 1 action_id
        FROM ActionTable
        WHERE rule_id = ?
          AND device_id = ?
          AND action_type = ?
    """;

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, ruleId);
            ps.setInt(2, deviceId);
            ps.setString(3, command.contains(" [") ? command.substring(0, command.indexOf(" [")) : command);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("action_id");
                }
            }
        }

        return -1;
    }


    public static List<ReadableLogResponse> loadReadableLogs() {
        List<ReadableLogResponse> logs = new ArrayList<>();

        String query = """
        SELECT TOP 50
            el.execution_time,
            el.status,
            r.rule_name,
            e.event_type,
            e.value,
            a.action_type,
            d.device_name,
            c.operator,
            c.value AS threshold
        FROM ExecutionLog el
        JOIN Rules r ON el.rule_id = r.rule_id
        JOIN Event e ON el.event_id = e.event_id
        JOIN ActionTable a ON el.action_id = a.action_id
        JOIN Device d ON a.device_id = d.device_id
        OUTER APPLY (SELECT TOP 1 operator, value FROM ConditionTable
                     WHERE rule_id = r.rule_id AND sensor_id = e.sensor_id ORDER BY condition_id) c
        ORDER BY el.execution_time DESC 
    """;

        try (Connection conn = DatabaseConfig.openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String action = buildReadableAction(
                        rs.getString("device_name"),
                        rs.getString("action_type")
                );

                String reason = buildReadableReason(
                        rs.getString("event_type"),
                        rs.getDouble("value"),
                        rs.getString("operator"),
                        rs.getDouble("threshold")
                );

                logs.add(new ReadableLogResponse(
                        rs.getTimestamp("execution_time").toString(),
                        action,
                        reason,
                        rs.getString("rule_name"),
                        rs.getString("status")
                ));
            }

        } catch (Exception e) {
            throw new IllegalStateException("Database operation failed", e);
        }

        return logs;
    }


    private static String buildReadableAction(String deviceName, String actionType) {
        String normalized = actionType == null ? "" : actionType.toLowerCase();

        if (normalized.contains("turn_on") || normalized.contains("on")) {
            return deviceName + " turned ON";
        }

        if (normalized.contains("turn_off") || normalized.contains("off")) {
            return deviceName + " turned OFF";
        }

        if (normalized.contains("send_alert")) {
            return "Alert sent from " + deviceName;
        }

        if (normalized.contains("shut_off")) {
            return deviceName + " shut OFF";
        }

        return deviceName + " executed action";
    }

    private static String buildReadableReason(String eventType, double value, String operator, double threshold) {
        if (eventType == null) {
            return "System event detected";
        }

        String op = operator == null ? "" : operator.trim();
        if (op.isBlank()) {
            return eventType + " reading " + formatNumber(value) + " was received; all rule conditions were evaluated";
        }

        String conditionText = switch (op) {
            case ">" -> "above the " + formatNumber(threshold) + " threshold";
            case "<" -> "below the " + formatNumber(threshold) + " threshold";
            case ">=" -> "at or above the " + formatNumber(threshold) + " threshold";
            case "<=" -> "at or below the " + formatNumber(threshold) + " threshold";
            case "=", "==" -> "equal to the " + formatNumber(threshold) + " required value";
            case "<>", "!=" -> "not equal to the " + formatNumber(threshold) + " restricted value";
            default -> "changed";
        };

        return switch (eventType.toLowerCase()) {
            case "motion" -> "Motion was detected";
            case "smoke" -> "Smoke was detected";
            case "gas" -> "Gas was detected";
            case "water leak" -> "Water leak was detected";

            case "temperature" -> "Temperature was " + formatNumber(value) + "°C, which is " + conditionText;

            case "power consumption" -> "Power consumption was " + formatNumber(value) + ", which is " + conditionText;

            default -> eventType + " triggered";
        };
    }

    private static String formatNumber(double number) {
        if (number == (int) number) {
            return String.valueOf((int) number);
        }
        return String.valueOf(number);
    }


    public static void updateRuleStatus(int ruleId, String status) {
        String query = """
        UPDATE Rules
        SET status = ?
        WHERE rule_id = ?
    """;

        try (Connection conn = DatabaseConfig.openConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, status);
            ps.setInt(2, ruleId);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new IllegalStateException("Database operation failed", e);
        }
    }



    public static List<ReadableLogResponse> loadReadableLogsByHome(int homeId) {
        List<ReadableLogResponse> logs = new ArrayList<>();

        String query = """
        SELECT TOP 50
            el.execution_time,
            el.status,
            r.rule_name,
            e.event_type,
            e.value,
            a.action_type,
            d.device_name,
            c.operator,
            c.value AS threshold
        FROM ExecutionLog el
        JOIN Rules r ON el.rule_id = r.rule_id
        JOIN Event e ON el.event_id = e.event_id
        JOIN ActionTable a ON el.action_id = a.action_id
        JOIN Device d ON a.device_id = d.device_id
        OUTER APPLY (SELECT TOP 1 operator, value FROM ConditionTable
                     WHERE rule_id = r.rule_id AND sensor_id = e.sensor_id ORDER BY condition_id) c
        WHERE r.home_id = ?
        ORDER BY el.execution_time DESC
    """;

        try (Connection conn = DatabaseConfig.openConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, homeId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String action = buildReadableAction(
                            rs.getString("device_name"),
                            rs.getString("action_type")
                    );

                    String reason = buildReadableReason(
                            rs.getString("event_type"),
                            rs.getDouble("value"),
                            rs.getString("operator"),
                            rs.getDouble("threshold")
                    );

                    logs.add(new ReadableLogResponse(
                            rs.getTimestamp("execution_time").toString(),
                            action,
                            reason,
                            rs.getString("rule_name"),
                            rs.getString("status")
                    ));
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("Database operation failed", e);
        }

        return logs;
    }


    public static HomeDashboardResponse loadHomeDashboard(int homeId) {
        int rooms = 0;
        int devices = 0;
        int activeDevices = 0;
        int activeRules = 0;
        int recentLogs = 0;

        try (Connection conn = DatabaseConfig.openConnection()) {

            rooms = count(conn, "SELECT COUNT(*) FROM Room WHERE home_id = ?", homeId);

            devices = count(conn, """
            SELECT COUNT(*)
            FROM Device d
            JOIN Room r ON d.room_id = r.room_id
            WHERE r.home_id = ?
        """, homeId);

            activeDevices = count(conn, """
            SELECT COUNT(*)
            FROM Device d
            JOIN Room r ON d.room_id = r.room_id
            WHERE r.home_id = ? AND d.is_active = 1
        """, homeId);

            activeRules = count(conn, """
            SELECT COUNT(*)
            FROM Rules
            WHERE home_id = ? AND status = 'active'
        """, homeId);

            recentLogs = count(conn, """
            SELECT COUNT(*)
            FROM ExecutionLog el
            JOIN ActionTable a ON el.action_id = a.action_id
            JOIN Device d ON a.device_id = d.device_id
            JOIN Room r ON d.room_id = r.room_id
            WHERE r.home_id = ? AND el.execution_time >= DATEADD(day, -1, GETDATE())
        """, homeId);

        } catch (Exception e) {
            throw new IllegalStateException("Database operation failed", e);
        }

        return new HomeDashboardResponse(rooms, devices, activeDevices, activeRules, recentLogs);
    }


    private static int count(Connection conn, String query, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }


    public static String getDeviceNameById(int deviceId) {
        String query = "SELECT device_name FROM Device WHERE device_id = ?";

        try (Connection conn = DatabaseConfig.openConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, deviceId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("device_name");
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("Database operation failed", e);
        }

        return "Device " + deviceId;
    }
}