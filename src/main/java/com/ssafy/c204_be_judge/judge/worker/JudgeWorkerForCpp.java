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
public class JudgeWorkerForCpp extends JudgeWorker {

    private static final String CPP_FILE_NAME = "Main.cpp";

    @PostConstruct
    public void init() {
        log.debug("JudgeWorkerForCpp initialized with THREAD_POOL_SIZE={}", MAX_THREAD_POOL_SIZE);
    }

    @Override
    protected String getFileName() {
        return CPP_FILE_NAME;
    }

    @Override
    protected List<String> getCompileCommand(String filePath) {
        return List.of(
            "g++", filePath, "-o", getCompiledFilePath(filePath), "-O2", "-Wall", "-lm", "-static", "-std=gnu++17"
        );
    }

    @Override
    protected String getCompiledFilePath(String filePath) {
        return filePath.replaceAll("\\.cpp$", "");
    }

    @Override
    protected List<String> getRunCommand(JudgeCommand judgeCommand, int boxId, int testcaseNum) {
        final String testcasePath = HOME_DIR + "/" + TESTCASE_PATH + judgeCommand.problemId();

        return List.of(
            "sudo",
            "isolate",
            "--box-id=" + boxId,
            "--processes=128",
            "--dir=" + testcasePath,
            "--stdin=" + testcasePath + "/" + testcaseNum + ".in",
            "--stdout=" + testcaseNum + OUTPUT_FILE_SUFFIX,
            "--stderr=" + testcaseNum + ERROR_FILE_SUFFIX,
            "--time=" + judgeCommand.timeLimit(),
            "--mem=" + 16384000, //16GB
            "--meta=" + "meta/" + testcaseNum + META_FILE_SUFFIX,
            "--run",
            "--",
            "./Main"
        );
    }
}
