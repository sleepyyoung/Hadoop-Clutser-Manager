package com.philpy.controller;

import com.alibaba.fastjson.JSON;
import com.philpy.utils.ResultMapToJson;
import com.philpy.utils.ShellUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/shell")
@RestController
public class ShellController {
    private static class GetJsonResult extends ResultMapToJson {
        public GetJsonResult(String msg, String detail) {
            super(msg, detail);
        }
    }

    @Autowired
    private ShellUtil shell;

    @GetMapping("/get-jps/{host}")
    public String getJps(@PathVariable("host") String host) {
        String detail, msg;
        try {
            msg = "success";
            detail = JSON.toJSONString(shell.execCommand(host, "source /etc/profile;source ~/.bash_profile;source ~/.bashrc;jps|grep -v Jps;"));
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }

    @PostMapping("/exec-command/{host}")
    public String exexCommand(@PathVariable("host") String host,
                              @RequestParam("command") String command) {
        String detail, msg;
        try {
            msg = "success";
            detail = JSON.toJSONString(shell.execCommand(host, "source /etc/profile;source ~/.bash_profile;source ~/.bashrc;" + command));
        } catch (Exception e) {
            msg = "error";
            detail = e.toString();
        }
        return new GetJsonResult(msg, detail).getResult();
    }

}
