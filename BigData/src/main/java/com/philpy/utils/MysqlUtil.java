package com.philpy.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

@Component
public class MysqlUtil {
    @Autowired
    private final MySQLConnectionSignal mysql = MySQLConnectionSignal.getInstance();

    @Value("${mysql.url}")
    private String url;

    @Value("${mysql.user}")
    private String user;

    @Value("${mysql.password}")
    private String password;

    public MysqlUtil() {
    }

    public Connection getConnection() throws SQLException {
        mysql.setConnection(url, user, password);
        return mysql.getConnection();
    }
}
