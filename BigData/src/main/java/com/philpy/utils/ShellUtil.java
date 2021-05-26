package com.philpy.utils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class ShellUtil {
    @Value("${shell.username}")
    private String username;

    @Value("${shell.password}")
    private String password;

    @Value("${shell.port}")
    private int port;

    private final ShellConnectionSignal shell = ShellConnectionSignal.getInstance();

    public ShellUtil() {
    }

    public List<String> execCommand(String host, String command) throws JSchException, IOException {
        List<String> list = new ArrayList<>();
        shell.setSession(host, port, username, password);
        Session session = shell.getSession();
        session.connect();
        shell.setExec(command, session);
        ChannelExec exec = shell.getExec();
        exec.connect();
        InputStream inputStream = exec.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String s;
        while ((s = bufferedReader.readLine()) != null) {
            list.add(s);
        }
        bufferedReader.close();
        exec.disconnect();
        session.disconnect();
        return list;
    }

}
