package com.ssafy.c204_be_judge.judge.strategy;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.domain.ProgrammingLanguage;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JavaJudgeStrategy implements JudgeStrategy {

    private static final String JAVA_FILE_NAME = "Main.java";

    @Override
    public boolean supports(String programmingLanguage) {
        return ProgrammingLanguage.isJava(programmingLanguage);
    }

    @Override
    public ProgrammingLanguage getLanguage() {
        return ProgrammingLanguage.JAVA;
    }

    @Override
    public String getFileName() {
        return JAVA_FILE_NAME;
    }

    @Override
    public List<String> getCompileCommand(String filePath) {
        return List.of("javac", "-J-Xms1024m", "-J-Xmx1920m", "-J-Xss512m", "-encoding", "UTF-8", filePath);
    }

    @Override
    public String getCompiledFilePath(String filePath) {
        return filePath.replaceAll("\\.java$", ".class");
    }

    @Override
    public List<String> getRunCommand(JudgeCommand judgeCommand, int boxId, int testcaseNum) {
        final String testcasePath = HOME_DIR + "/" + TESTCASE_PATH + judgeCommand.problemId();

        return List.of(
            "sudo",
            "isolate",
            "--box-id=" + boxId,
            "--processes=128",
            "--dir=/usr/lib/jvm",
            "--dir=/etc/java-17-openjdk/security",
            "--dir=" + testcasePath,
            "--stdin=" + testcasePath + "/" + testcaseNum + ".in",
            "--stdout=" + testcaseNum + OUTPUT_FILE_SUFFIX,
            "--stderr=" + testcaseNum + ERROR_FILE_SUFFIX,
            "--time=" + (judgeCommand.timeLimit() * 2 + 1), // 2배 + 1초 여유
            "--mem=" + 16384000, // 16GB
            "--meta=" + "meta/" + testcaseNum + META_FILE_SUFFIX,
            "--run",
            "--",
            "/usr/lib/jvm/java-17-openjdk-arm64/bin/java",
            "-Xms1024m", "-Xmx1920m", "-Xss512m", "-Dfile.encoding=UTF-8", "-XX:+UseSerialGC",
            "Main"
        );
    }
}
