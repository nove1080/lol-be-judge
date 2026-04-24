package com.ssafy.c204_be_judge.judge.worker;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.validation.exception.CompileException;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Ubuntu 환경에서 채점을 수행하는 JudgeWorker 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeWorkerForPython extends JudgeWorker {

    @PostConstruct
    public void init() {
        log.debug("JudgeWorkerForPython initialized with THREAD_POOL_SIZE={}", MAX_THREAD_POOL_SIZE);
    }

    @Override
    protected String getFileName() {
        return "Main.py";
    }

    @Override
    protected List<String> getCompileCommand(String filePath) {
        return List.of("python3", "-W", "ignore", "-c", "import py_compile; py_compile.compile(r'" + filePath + "')");
    }

    @Override
    protected String getCompiledFilePath(String filePath) {
        return filePath;
    }

    @Override
    protected List<String> getRunCommand(JudgeCommand judgeCommand, int boxId, int testcaseNum) {
        final String testcasePath = HOME_DIR + "/" + TESTCASE_PATH + judgeCommand.problemId();

        return List.of(
            "sudo",
            "isolate",
            "--box-id=" + boxId,
            "--processes=128",
            "--dir=/usr/bin/python3.12",
            "--dir=" + testcasePath,
            "--stdin=" + testcasePath + "/" + testcaseNum + ".in",
            "--stdout=" + testcaseNum + OUTPUT_FILE_SUFFIX,
            "--stderr=" + testcaseNum + ERROR_FILE_SUFFIX,
            "--time=" + judgeCommand.timeLimit() * 3 + 2, // 3배 + 2초 여유
            "--mem=" + 16384000, //16GB
            "--meta=" + "meta/" + testcaseNum + META_FILE_SUFFIX,
            "--run",
            "--",
            "/usr/bin/python3.12",
            "-W", "ignore",
            "Main.py"
        );
    }
}
