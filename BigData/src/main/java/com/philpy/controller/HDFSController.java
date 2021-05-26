package com.philpy.controller;

import com.alibaba.fastjson.JSON;
import com.philpy.utils.ResultMapToJson;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.io.FilenameUtils;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/api/hdfs")
@RestController
public class HDFSController {
    private static class GetJsonResult extends ResultMapToJson {
        public GetJsonResult(String msg, String detail) {
            super(msg, detail);
        }
    }

    @Value("${hdfs.path}")
    private String hdfsPath;

    @Value("${hdfs.username}")
    private String hdfsUser;

    private final String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getPath();

    @GetMapping("/getFileContent")
    public String getFileContent(@RequestParam("fileName") String fileName) {
        List<String> txts = new ArrayList<>();
        txts.add("txt");
        txts.add("out");
        txts.add("log");
        txts.add("xml");
        txts.add("yml");
        txts.add("html");
        txts.add("htm");
        txts.add("md");
        txts.add("js");
        txts.add("css");
        txts.add("java");
        String detail, msg;
        Configuration conf = new Configuration();
        conf.set("fs.default.name", hdfsPath);
        Path filePath = new Path(fileName);
        try (FileSystem fs = FileSystem.get(new URI(hdfsPath), conf, hdfsUser)) {
            if (!fs.exists(filePath)) {
                fs.close();
                return new GetJsonResult("warning", "文件 " + fileName + " 不存在！").getResult();
            }
            if (fs.isDirectory(filePath)) {
                fs.close();
                return new GetJsonResult("warning", "文件 " + fileName + " 是一个文件夹，无法预览！").getResult();
            }
            if (!txts.contains(FilenameUtils.getExtension(fileName).toLowerCase())) {
                return new GetJsonResult("warning", "文件 " + fileName + " 不可预览，请下载后查看！").getResult();
            }
            FSDataInputStream inputStream = fs.open(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String lineTxt;
            StringBuilder sb = new StringBuilder();
            while ((lineTxt = reader.readLine()) != null) {
                sb.append("<pre>").append(lineTxt.replace("<", "&lt;").replace(">", "&gt;")).append("</pre>");
            }
            msg = "success";
            detail = sb.toString();
            inputStream.close();
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }

    @PostMapping("/createDir")
    public String createDir(@RequestParam("dirPath") String dirPath) {
        String detail = "", msg;
        Configuration conf = new Configuration();
        conf.set("fs.default.name", hdfsPath);
        try (FileSystem fs = FileSystem.get(new URI(hdfsPath), conf, hdfsUser)) {
            Path path = new Path(dirPath);
            if (!fs.exists(path)) {
                if (fs.mkdirs(path)) {
                    msg = "success";
                } else {
                    msg = "warning";
                    detail = "创建失败，未知错误！";
                }
            } else {
                msg = "warning";
                detail = "该目录已存在！";
            }
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }

    @PostMapping("/uploadFile")
    public String uploadFile(@RequestParam("localFile") String localFile,
                             @RequestParam("hdfsDir") String hdfsDir) {
        String detail = "", msg;
        Configuration conf = new Configuration();
        conf.set("fs.default.name", hdfsPath);
        try (FileSystem fs = FileSystem.get(new URI(hdfsPath), conf, hdfsUser)) {
            fs.copyFromLocalFile(new Path(localFile), new Path(hdfsDir));
            msg = "success";
        } catch (PathIsDirectoryException e) {
            msg = "error";
            detail = localFile + " 是一个文件夹！请选择一个文件进行上传！";
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }

    private void showDir(FileStatus fs, FileSystem hdfs, List<Map<String, String>> list) throws IOException {
        Path path = fs.getPath();
        Map<String, String> map = new HashMap<>();
        map.put("pathName", path.toString().replace(hdfsPath, ""));
        map.put("type", fs.isDirectory() ? "文件夹" : "文件");
        list.add(map);
        if (fs.isDirectory()) {
            FileStatus[] f = hdfs.listStatus(path);
            if (f.length > 0) {
                for (FileStatus file : f) {
                    showDir(file, hdfs, list);
                }
            }
        }
    }

    @GetMapping("/scanDirs")
    public String scanDirs(@RequestParam("hdfsDir") String hdfsDir) throws IOException, URISyntaxException, InterruptedException {
        List<Map<String, String>> list = new ArrayList<>();
        Configuration conf = new Configuration();
        conf.set("fs.default.name", hdfsPath);
        FileSystem hdfs = FileSystem.get(new URI(hdfsPath), conf, hdfsUser);
        FileStatus[] fs = hdfs.listStatus(new Path(hdfsDir));
        if (fs.length > 0) {
            for (FileStatus f : fs) {
                showDir(f, hdfs, list);
            }
        }
        hdfs.close();
        Map<String, Object> map = new HashMap<>();
        map.put("data", list);
        map.put("code", 0);
        return JSON.toJSONString(map);
    }

    @PostMapping("/deleteResource")
    public String deleteResource(@RequestParam("resourcePath") String resourcePath) {
        String detail = "", msg;
        Configuration conf = new Configuration();
        conf.set("fs.default.name", hdfsPath);
        try (FileSystem fs = FileSystem.get(new URI(hdfsPath), conf, hdfsUser)) {
            if (fs.deleteOnExit(new Path(resourcePath))) {
                msg = "success";
            } else {
                msg = "warning";
                detail = "资源 " + resourcePath + " 不存在！";
            }
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }

    @PostMapping("/downloadResource")
    public String downloadResource(@RequestParam("resourcePath") String resourcePath) {
        String detail = "", msg;
        Configuration conf = new Configuration();
        conf.set("fs.default.name", hdfsPath);
        try (FileSystem fs = FileSystem.get(new URI(hdfsPath), conf, hdfsUser)) {
            fs.copyToLocalFile(false, new Path(resourcePath), new Path(desktopPath), true);
            msg = "success";
        } catch (IOException e) {
            msg = "warning";
            detail = "下载失败，桌面上已经有了一个名为 ”" + resourcePath + "“的文件夹！";
        } catch (Exception e) {
            msg = "warning";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }
}