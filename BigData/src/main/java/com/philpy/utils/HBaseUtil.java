package com.philpy.utils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HBaseUtil {
    static private Connection connection;
    static private final Configuration conf = new Configuration();

    @Value("${zookeeper.quorum}")
    private String quorum;

    public Connection getConnection() throws IOException {
        conf.set("hbase.zookeeper.quorum", quorum);
        connection = ConnectionFactory.createConnection(conf);
        return connection;
    }

    public void close() throws IOException {
        connection.close();
    }

}
