package com.ssafy.c204_be_judge.judge.service;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.springframework.stereotype.Service;

@Service
public class JudgeService {

    private static final String TESTCASE_DIR = "/home/ubuntu/testcases";

    private void sendSourceCode(JudgeCommand judgeCommand) {
        String filePath = "/home/ubuntu/sourceCode/" + judgeCommand.problemId() + "/Main.java";

        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    private void sendTestcase(JudgeCommand judgeCommand) {

    }

    public void judge(JudgeCommand judgeCommand) throws IOException {
        sendSourceCode(judgeCommand);
        sendTestcase(judgeCommand);

        ProcessBuilder pb = new ProcessBuilder(
            "isolate",
            "--box-id=" + judgeCommand.problemId().toString(),
            "--processes=128",
            "--dir=/usr/lib/jvm",
            "--dir=/etc/java-17-openjdk/security",
            "--run",
            "--",
            "/usr/lib/jvm/java-17-openjdk-amd64/bin/javac",
            "-encoding", "UTF-8",
            "Main.java"
        );
        pb.directory(new File(TESTCASE_DIR));
        pb.command("ls", "-al");

        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }

}
