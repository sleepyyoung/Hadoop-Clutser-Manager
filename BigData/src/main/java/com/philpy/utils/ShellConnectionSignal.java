package com.philpy.utils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.springframework.stereotype.Component;

@Component
public class ShellConnectionSignal {
    final private JSch jSch = new JSch();
    private Session session;
    private ChannelExec exec;

    public void setSession(String host, int port, String username, String password) throws JSchException {
        this.session = jSch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
    }

    public Session getSession() {
        return session;
    }

    public void setExec(String command, Session session) throws JSchException {
        this.exec = (ChannelExec) session.openChannel("exec");
        exec.setCommand(command);
    }

    public ChannelExec getExec() {
        return exec;
    }

    private ShellConnectionSignal() {
    }

    public volatile static ShellConnectionSignal instance = null;

    public static ShellConnectionSignal getInstance() {
        if (instance == null) {
            synchronized (ShellConnectionSignal.class) {
                if (instance == null) {
                    instance = new ShellConnectionSignal();
                }
            }
        }
        return instance;
    }
}
