package com.philpy.utils;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

public abstract class ResultMapToJson {
    private Map<String, Object> map = new HashMap<>();

    public ResultMapToJson(String msg, String detail) {
        map.put("msg", msg);
        map.put("detail", detail);
    }

    public ResultMapToJson(Map<String, Object> map) {
        this.map = map;
    }

    public String getResult() {
        return JSON.toJSONString(this.map);
    }
}
