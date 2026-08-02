package com.ssafy.c204_be_judge.judge.strategy;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.domain.ProgrammingLanguage;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PythonJudgeStrategy implements JudgeStrategy {

    @Override
    public boolean supports(String programmingLanguage) {
        return ProgrammingLanguage.isPython(programmingLanguage);
    }

    @Override
    public ProgrammingLanguage getLanguage() {
        return ProgrammingLanguage.PYTHON;
    }

    @Override
    public String getFileName() {
        return "Main.py";
    }

    @Override
    public List<String> getCompileCommand(String filePath) {
        return List.of("python3", "-W", "ignore", "-c", "import py_compile; py_compile.compile(r'" + filePath + "')");
    }

    @Override
    public String getCompiledFilePath(String filePath) {
        return filePath;
    }

    @Override
    public List<String> getRunCommand(JudgeCommand judgeCommand, int boxId, int testcaseNum) {
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
            "--time=" + (judgeCommand.timeLimit() * 3 + 2), // 3배 + 2초 여유
            "--mem=" + 16384000, // 16GB
            "--meta=" + "meta/" + testcaseNum + META_FILE_SUFFIX,
            "--run",
            "--",
            "/usr/bin/python3.12",
            "-W", "ignore",
            "Main.py"
        );
    }
}
