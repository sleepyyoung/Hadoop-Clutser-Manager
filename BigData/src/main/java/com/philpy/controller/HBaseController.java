package com.philpy.controller;

import com.alibaba.fastjson.JSON;
import com.philpy.utils.HBaseUtil;
import com.philpy.utils.ResultMapToJson;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/api/hbase")
@RestController
public class HBaseController {
    private static class GetJsonResult extends ResultMapToJson {
        public GetJsonResult(String msg, String detail) {
            super(msg, detail);
        }
    }

    @Autowired
    private HBaseUtil hBaseUtil;

    @GetMapping("/tables")
    public String showTables() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> list = new ArrayList<>();
        try (Connection connection = hBaseUtil.getConnection();
             Admin admin = connection.getAdmin()) {
            TableName[] tableNames = admin.listTableNames();
            for (TableName tableName : tableNames) {
                Map<String, String> map = new HashMap<>();
                map.put("tableName", tableName.getNameAsString());
                list.add(map);
            }
            result.put("data", list);
            result.put("code", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return JSON.toJSONString(result);
    }

    @PostMapping("/create-table")
    public String createTable(@RequestParam("tableName") String tableName,
                              @RequestParam("familyNames") String familyNames) {
        String detail = "", msg;
        try (Connection connection = hBaseUtil.getConnection();
             Admin admin = connection.getAdmin()) {
            HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf(tableName));
            for (Object familyName : JSON.parseArray(familyNames)) {
                tableDescriptor.addFamily(new HColumnDescriptor(familyName.toString()));
            }
            admin.createTable(tableDescriptor);
            msg = "success";
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }

    @PostMapping("/delete-family")
    public String deleteFamily(@RequestParam("tableName") String tableName,
                               @RequestParam("familyNames") String familyNames) {
        String detail = "", msg;
        try (Connection connection = hBaseUtil.getConnection();
             Admin admin = connection.getAdmin()) {
            for (Object familyName : JSON.parseArray(familyNames)) {
                String f = (String) familyName;
                admin.deleteColumn(TableName.valueOf(tableName), f.getBytes(StandardCharsets.UTF_8));
            }
            msg = "success";
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }

    @PostMapping("/add-family")
    public String addFamily(@RequestParam("rowKey") String rowKey,
                            @RequestParam("tableName") String tableName,
                            @RequestParam("familyName") String familyName,
                            @RequestParam("keys") String keys,
                            @RequestParam("values") String values) {
        String detail = "", msg;
        List<String> rowKeys = new ArrayList<>();
        try (Connection connection = hBaseUtil.getConnection();
             Admin admin = connection.getAdmin();
             Table table = connection.getTable(TableName.valueOf(tableName))) {
            Scan scan = new Scan();
            ResultScanner rss = table.getScanner(scan);
            for (Result r : rss) {
                rowKeys.add(new String(r.getRow()));
            }
            Object[] keyArray = JSON.parseArray(keys).toArray();
            Object[] valueArray = JSON.parseArray(values).toArray();
            admin.addColumn(TableName.valueOf(tableName), new HColumnDescriptor(familyName));
            assert JSON.parseArray(keys).size() == JSON.parseArray(values).size();
            if (!rowKey.equals("")) {
                Put put = new Put(Bytes.toBytes(rowKey));
                for (int i = 0; i < JSON.parseArray(keys).size(); i++) {
                    put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(keyArray[i].toString()), Bytes.toBytes(valueArray[i].toString()));
                    System.out.println(keyArray[i] + "  " + valueArray[i]);
                }
                table.put(put);
            } else {
                for (String r : rowKeys) {
                    Put put = new Put(Bytes.toBytes(r));
                    for (int i = 0; i < JSON.parseArray(keys).size(); i++) {
                        put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(keyArray[i].toString()), Bytes.toBytes(valueArray[i].toString()));
                        System.out.println(keyArray[i] + "  " + valueArray[i]);
                    }
                    table.put(put);
                }
            }
            msg = "success";
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }

    @PostMapping("/add-data")
    public String addData(@RequestParam("rowKey") String rowKey,
                          @RequestParam("tableName") String tableName,
                          @RequestParam("familyName") String familyName,
                          @RequestParam("keys") String keys,
                          @RequestParam("values") String values) {
        String detail = "", msg;
        List<String> rowKeys = new ArrayList<>();
        try (Connection connection = hBaseUtil.getConnection();
             Admin admin = connection.getAdmin();
             Table table = connection.getTable(TableName.valueOf(tableName))) {
            Scan scan = new Scan();
            ResultScanner rss = table.getScanner(scan);
            for (Result r : rss) {
                rowKeys.add(new String(r.getRow()));
            }
            Object[] keyArray = JSON.parseArray(keys).toArray();
            Object[] valueArray = JSON.parseArray(values).toArray();
            try {
                admin.addColumn(TableName.valueOf(tableName), new HColumnDescriptor(familyName));
            } catch (Exception ignored) {
            }
            assert JSON.parseArray(keys).size() == JSON.parseArray(values).size();
            if (!rowKey.equals("")) {
                Put put = new Put(Bytes.toBytes(rowKey));
                for (int i = 0; i < JSON.parseArray(keys).size(); i++) {
                    put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(keyArray[i].toString()), Bytes.toBytes(valueArray[i].toString()));
                    System.out.println(keyArray[i] + "  " + valueArray[i]);
                }
                table.put(put);
            } else {
                for (String r : rowKeys) {
                    Put put = new Put(Bytes.toBytes(r));
                    for (int i = 0; i < JSON.parseArray(keys).size(); i++) {
                        put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(keyArray[i].toString()), Bytes.toBytes(valueArray[i].toString()));
                        System.out.println(keyArray[i] + "  " + valueArray[i]);
                    }
                    table.put(put);
                }
            }
            msg = "success";
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }

    @PostMapping("/delete-row")
    public String deleteRow(@RequestParam("rowKeys") String sRowKeys,
                            @RequestParam("tableName") String tableName) {
        String detail = "", msg;
        List<String> rowKeys = new ArrayList<>();
        try (Connection connection = hBaseUtil.getConnection();
             Table table = connection.getTable(TableName.valueOf(tableName))) {
            Scan scan = new Scan();
            ResultScanner rss = table.getScanner(scan);
            for (Result r : rss) {
                rowKeys.add(new String(r.getRow()));
            }
            if (sRowKeys.replace(" ", "").equals("[]")) {
                for (String rowKey : rowKeys) {
                    table.delete(new Delete(Bytes.toBytes(rowKey)));
                }
            } else {
                JSON.parseArray(sRowKeys).forEach((row) -> {
                    try {
                        table.delete(new Delete(Bytes.toBytes(row.toString())));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            msg = "success";
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }

    @PostMapping("/delete-table")
    public String deleteTable(@RequestParam("tableName") String tableName) {
        String detail = "", msg;
        try (Connection connection = hBaseUtil.getConnection();
             Admin admin = connection.getAdmin()) {
            TableName t = TableName.valueOf(tableName);
            admin.disableTable(t);
            admin.deleteTable(t);
            msg = "success";
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }
}