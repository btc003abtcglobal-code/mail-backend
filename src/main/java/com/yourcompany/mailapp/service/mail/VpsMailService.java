package com.yourcompany.mailapp.service.mail;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VpsMailService {

    @Value("${vps.host}")
    private String host;

    @Value("${vps.username}")
    private String username;

    @Value("${vps.password}")
    private String password;

    public void createMailbox(String mailName, String mailPassword) throws Exception {

        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, 22);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        // Ensure the command is correct. User provided:
        // /usr/local/bin/create-mail-user.sh
        String command = "/usr/local/bin/create-mail-user.sh "
                + mailName + " " + mailPassword;

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.connect();

        while (!channel.isClosed())
            Thread.sleep(100);

        if (channel.getExitStatus() != 0)
            throw new RuntimeException("Mail user creation failed. Exit status: " + channel.getExitStatus());

        channel.disconnect();
        session.disconnect();
    }
}
