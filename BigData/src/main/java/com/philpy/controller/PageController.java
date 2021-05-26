package com.philpy.controller;

import com.philpy.utils.HBaseUtil;
import com.philpy.utils.ListUtil;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.stereotype.Controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequestMapping("/")
@Controller
public class PageController {
    @Autowired
    private HBaseUtil hBaseUtil;

    @GetMapping("/")
    public String getIndex() {
        return "redirect:shell";
    }

    @GetMapping("/shell")
    public String shellTest() {
        return "shell";
    }

    @GetMapping("/hdfs")
    public String hdfsTest() {
        return "hdfs";
    }

    @GetMapping("/kafka")
    public String kafkaTest() {
        return "kafka";
    }

    @GetMapping("/mapreduce")
    public String mapreduceTest() {
        return "mapreduce";
    }

    @GetMapping("/hbase")
    public String hbaseTest() {
        return "hbase";
    }

    @GetMapping("/hbase/create-table")
    public String hbaseTestCT() {
        return "hbase_create_table";
    }

    @GetMapping("/hbase/delete-family")
    public String hbaseTestDF() {
        return "hbase_delete_family";
    }

    @GetMapping("/hbase/add-data")
    public String hbaseTestAD() {
        return "hbase_add_data";
    }

    @GetMapping("/hbase/delete-row")
    public String hbaseTestDR() {
        return "hbase_delete_row";
    }

    @GetMapping("/hbase/{table_ame}")
    public String hbaseTest(@PathVariable("table_ame") String tableName, Model model) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<String> allFamilyNames = new ArrayList<>();
        List<String> rowKeys = new ArrayList<>();
        try (Connection connection = hBaseUtil.getConnection();
             Table table = connection.getTable(TableName.valueOf(tableName))) {
            Scan scan = new Scan();
            ResultScanner rss = table.getScanner(scan);
            for (Result r : rss) {
                rowKeys.add(new String(r.getRow()));
            }
            HTableDescriptor hTableDescriptor = table.getTableDescriptor();
            for (HColumnDescriptor hColumnDescriptor : hTableDescriptor.getColumnFamilies()) {
                allFamilyNames.add(hColumnDescriptor.getNameAsString());
            }
            for (String rowKey : rowKeys) {
                List<String> notEmptyFamilyNames = new ArrayList<>();
                Map<String, Object> oneRow = new HashMap<>();
                List<String> qualifiers = new ArrayList<>();
                List<String> values = new ArrayList<>();
                List<Map<String, String>> families = new ArrayList<>();
                oneRow.put("rowKey", rowKey);
                for (Cell cell : table.get(new Get(rowKey.getBytes(StandardCharsets.UTF_8))).rawCells()) {
                    String family = new String(CellUtil.cloneFamily(cell));
                    notEmptyFamilyNames.add(family);
                    String qualifier = new String(CellUtil.cloneQualifier(cell));
                    qualifiers.add(qualifier);
                    String value = new String(CellUtil.cloneValue(cell));
                    values.add(value);
                }
                for (String s : notEmptyFamilyNames.stream().distinct().collect(Collectors.toList())) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("name", s);
                    map.put("length", String.valueOf(ListUtil.getCount(notEmptyFamilyNames, s)));
                    families.add(map);
                }
                oneRow.put("families", families);
                oneRow.put("qualifiers", qualifiers);
                oneRow.put("values", values);
                results.add(oneRow);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("allFamilyNames", allFamilyNames);
        model.addAttribute("results", results);
        return "hbase_detail";
    }
}


