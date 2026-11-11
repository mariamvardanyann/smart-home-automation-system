
package com.smarthome.smart_home_backend.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {
    private DatabaseConfig() { }

    public static Connection openConnection() throws SQLException {
        String url = required("SMART_HOME_DB_URL");

        if (url.contains("integratedSecurity=true")
                || url.contains("authenticationScheme=NativeAuthentication")) {
            return DriverManager.getConnection(url);
        }

        String user = required("SMART_HOME_DB_USER");
        String password = required("SMART_HOME_DB_PASSWORD");

        return DriverManager.getConnection(url, user, password);
    }

    private static String required(String name) throws SQLException {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new SQLException(
                "Missing required environment variable: " + name
            );
        }
        return value;
    }
}
