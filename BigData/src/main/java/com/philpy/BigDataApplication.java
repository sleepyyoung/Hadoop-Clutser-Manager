package com.philpy;

import com.philpy.config.InitConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@SpringBootApplication
public class BigDataApplication {
    public static void main(String[] args) throws IOException {
        System.setProperty("HADOOP_USER_NAME", "hadoop");
        ConfigurableApplicationContext applicationContext = SpringApplication.run(BigDataApplication.class, args);
        InitConfig initConfig = applicationContext.getBean(InitConfig.class);
        Runtime.getRuntime().exec(initConfig.getWordCountCmd());
    }
}
