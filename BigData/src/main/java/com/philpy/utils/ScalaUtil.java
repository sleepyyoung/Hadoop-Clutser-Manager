package com.philpy.utils;

import com.philpy.config.InitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class ScalaUtil {

    @Autowired
    private InitConfig initConfig;

    public void startStreamingKafkaWordCount() throws IOException {
        Runtime.getRuntime().exec(initConfig.getWordCountCmd());
    }

    public void stopStreamingKafkaWordCount() throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(initConfig.getJpsCmd()).getInputStream()))) {
            String s;
            while ((s = bufferedReader.readLine()) != null) {
                if (s.contains("StreamingKafkaWordCount")) {
                    Runtime.getRuntime().exec("taskkill /PID " + s.replace("StreamingKafkaWordCount", "") + " /F");
                }
            }
        }
    }

}
