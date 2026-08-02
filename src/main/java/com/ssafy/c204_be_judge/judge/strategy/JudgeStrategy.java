package com.ssafy.c204_be_judge.judge.strategy;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.domain.ProgrammingLanguage;
import java.util.List;

public interface JudgeStrategy {

    String HOME_DIR = "/home/ubuntu";
    String ISOLATE_ROOT = "/var/lib/isolate/";
    String TESTCASE_PATH = "testcases/";

    String OUTPUT_FILE_SUFFIX = "_output.txt";
    String ERROR_FILE_SUFFIX = "_err.txt";
    String META_FILE_SUFFIX = "_meta.txt";

    boolean supports(String programmingLanguage);

    ProgrammingLanguage getLanguage();

    String getFileName();

    List<String> getCompileCommand(String filePath);

    String getCompiledFilePath(String filePath);

    List<String> getRunCommand(JudgeCommand judgeCommand, int boxId, int testcaseNum);
}
