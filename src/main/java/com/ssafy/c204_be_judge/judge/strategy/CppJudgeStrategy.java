package com.ssafy.c204_be_judge.judge.strategy;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.domain.ProgrammingLanguage;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CppJudgeStrategy implements JudgeStrategy {

    private static final String CPP_FILE_NAME = "Main.cpp";

    @Override
    public boolean supports(String programmingLanguage) {
        return ProgrammingLanguage.isCpp(programmingLanguage);
    }

    @Override
    public ProgrammingLanguage getLanguage() {
        return ProgrammingLanguage.CPP;
    }

    @Override
    public String getFileName() {
        return CPP_FILE_NAME;
    }

    @Override
    public List<String> getCompileCommand(String filePath) {
        return List.of(
            "g++", filePath, "-o", getCompiledFilePath(filePath), "-O2", "-Wall", "-lm", "-static", "-std=gnu++17"
        );
    }

    @Override
    public String getCompiledFilePath(String filePath) {
        return filePath.replaceAll("\\.cpp$", "");
    }

    @Override
    public List<String> getRunCommand(JudgeCommand judgeCommand, int boxId, int testcaseNum) {
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
            "--mem=" + 16384000, // 16GB
            "--meta=" + "meta/" + testcaseNum + META_FILE_SUFFIX,
            "--run",
            "--",
            "./Main"
        );
    }
}
