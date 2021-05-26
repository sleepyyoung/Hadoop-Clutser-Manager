package com.philpy.controller;

import com.philpy.utils.MysqlUtil;
import com.philpy.utils.ResultMapToJson;
import com.philpy.utils.ScalaUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

@RequestMapping("/api/kafka")
@RestController
public class KafkaController {
    private static class GetJsonResult extends ResultMapToJson {
        public GetJsonResult(String msg, String detail) {
            super(msg, detail);
        }
    }

    private final SynchronousQueue<String> messageQueue = new SynchronousQueue<>();

    private final String topictest = "topictest", topictest2 = "topictest2";

    @Autowired
    ScalaUtil scalaUtil;

    @Autowired
    private MysqlUtil mysql;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @PostMapping("/send")
    public String send(@RequestParam("topic") String topic,
                       @RequestParam("message") String message) {
        String detail = "", msg;
        try {
            kafkaTemplate.send(topic, message);
            msg = "success";
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }

    @KafkaListener(topics = {topictest, topictest2})
    public void receiveMessage(ConsumerRecord<String, String> consumerRecord, Acknowledgment ack) throws InterruptedException {
        try {
            String value = consumerRecord.value();
            String topic = consumerRecord.topic();
            long timestamp = consumerRecord.timestamp();
            long offset = consumerRecord.offset();
            messageQueue.put("value: " + value + "       topic: " + topic + "       timestamp: " + timestamp + "       offset: " + offset);
        } finally {
            ack.acknowledge();
        }
    }

    @PostMapping("/receive")
    public String receive() {
        return messageQueue.poll();
    }

    @PostMapping("/wordcount")
    public String wordCount() throws SQLException {
        List<String> result = new ArrayList<>();
        try (Connection connection = mysql.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("select word, max(count) from wordcount group by word");
            while (rs.next()) {
                result.add("(" + rs.getString(1) + "," + rs.getInt(2) + ")");
            }

            if (result.size() == 0) {
                return "";
            }
            return result.toString();
        }
    }

    @PostMapping("/cleardb")
    public String clearDB() {
        String detail = "", msg;
        try (Connection connection = mysql.getConnection();
             Statement statement = connection.createStatement()) {
            scalaUtil.stopStreamingKafkaWordCount();
            statement.executeUpdate("truncate table wordcount");
            msg = "success";
            scalaUtil.startStreamingKafkaWordCount();
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }
}
