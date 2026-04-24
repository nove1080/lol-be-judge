package com.ssafy.c204_be_judge.judge.worker;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.validation.exception.CompileException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Ubuntu 환경에서 채점을 수행하는 JudgeWorker 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeWorkerForJava extends JudgeWorker {

    private static final String JAVA_FILE_NAME = "Main.java";

    @PostConstruct
    public void init() {
        log.debug("JudgeWorkerForJava initialized with THREAD_POOL_SIZE={}", MAX_THREAD_POOL_SIZE);
    }

    @Override
    protected String getFileName() {
        return JAVA_FILE_NAME;
    }

    @Override
    protected List<String> getCompileCommand(String filePath) {
        return List.of("javac", "-J-Xms1024m", "-J-Xmx1920m", "-J-Xss512m", "-encoding", "UTF-8", filePath);
    }

    @Override
    protected String getCompiledFilePath(String filePath) {
        return filePath.replaceAll("\\.java$", ".class");
    }

    @Override
    protected List<String> getRunCommand(JudgeCommand judgeCommand, int boxId, int testcaseNum) {
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
            "--time=" + judgeCommand.timeLimit() * 2 + 1, // 2배 + 1초 여유
            "--mem=" + 16384000, //16GB
            "--meta=" + "meta/" + testcaseNum + META_FILE_SUFFIX,
            "--run",
            "--",
            "/usr/lib/jvm/java-17-openjdk-arm64/bin/java",
            "-Xms1024m", "-Xmx1920m", "-Xss512m", "-Dfile.encoding=UTF-8", "-XX:+UseSerialGC",
            "Main"
        );
    }
}
