package com.philpy.controller;

import com.philpy.utils.ResultMapToJson;
import com.philpy.utils.WordCount;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/mapreduce")
public class MapReduceController {
    private static class GetJsonResult extends ResultMapToJson {
        public GetJsonResult(String msg, String detail) {
            super(msg, detail);
        }
    }

    @Autowired
    private WordCount wordCount;

    @Value("${hdfs.path}")
    private String hdfsPath;

    @Value("${hdfs.username}")
    private String hdfsUser;

    @PostMapping("/wordcount")
    public String doWordCount(@RequestParam("hdfsFile") String hdfsFile) {
        String detail , msg;
        Configuration conf = new Configuration();
        conf.set("fs.default.name", hdfsPath);
        try {
            String outputDir = "/output-" + System.currentTimeMillis();
            detail = wordCount.getWordCount(hdfsFile, outputDir);
            try (FileSystem fs = FileSystem.get(new URI(hdfsPath), conf, hdfsUser)) {
                if (fs.deleteOnExit(new Path(outputDir))) {
                    msg = "success";
                } else {
                    msg = "warning";
                    detail = "资源 " + outputDir + " 不存在！";
                }
            } catch (Exception e) {
                msg = "error";
                detail = e.toString();
            }
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }
}
