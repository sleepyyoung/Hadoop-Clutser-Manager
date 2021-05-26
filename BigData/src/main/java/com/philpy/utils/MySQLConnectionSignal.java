package com.philpy.utils;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class MySQLConnectionSignal {
    private Connection connection;

    public void setConnection(String url, String user, String password) throws SQLException {
        connection = DriverManager.getConnection(url, user, password);
    }

    public Connection getConnection() {
        return connection;
    }

    private MySQLConnectionSignal() {
    }

    private volatile static MySQLConnectionSignal instance = null;

    public static MySQLConnectionSignal getInstance() {
        if (instance == null) {
            synchronized (MySQLConnectionSignal.class) {
                if (instance == null) {
                    instance = new MySQLConnectionSignal();
                }
            }
        }
        return instance;
    }
}
