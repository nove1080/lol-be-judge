package com.ssafy.c204_be_judge.judge.worker;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.domain.JudgeResult;
import com.ssafy.c204_be_judge.judge.domain.TestcaseResult;
import com.ssafy.c204_be_judge.validation.exception.CompileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.*;

@Slf4j
public abstract class JudgeWorker {

    protected static final int MAX_THREAD_POOL_SIZE = Math.max(1, Runtime.getRuntime().availableProcessors());

    protected static final String HOME_DIR = "/home/ubuntu"; // /home/ubuntu
    protected static final String ISOLATE_ROOT = "/var/lib/isolate/";
    protected static final String TESTCASE_PATH = "testcases/";

    protected static final String OUTPUT_FILE_SUFFIX = "_output.txt";
    protected static final String ERROR_FILE_SUFFIX = "_err.txt";
    protected static final String META_FILE_SUFFIX = "_meta.txt";

    public abstract JudgeResult run(JudgeCommand judgeCommand);

    protected abstract String compile(String filePath) throws CompileException;

    protected abstract String executeSourceCode(JudgeCommand judgeCommand, int boxId, int testcaseNum);

    protected static void prepareSandbox(int boxId) {
        ProcessBuilder pb = new ProcessBuilder(
                "sudo",
                "isolate",
                "--box-id=" + boxId,
                "--init"
        );

        try {
            Process process = pb.start();
            logging(process);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected static int getTotalTestcaseCount(JudgeCommand judgeCommand) {
        final String testcasePath = HOME_DIR + "/testcases/" + judgeCommand.problemId();

        File dir = new File(testcasePath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("디렉토리가 유효하지 않습니다! [경로: %s]".formatted(testcasePath));
        }

        return dir.list((f, name) -> name.contains(".in")).length;
    }

    protected static String writeSourceCode(JudgeCommand judgeCommand, String fileName) {
        final String codePath = HOME_DIR + "/sourceCode/" + judgeCommand.problemId() + "/" + fileName;

        File file = new File(codePath);
        file.getParentFile().mkdirs();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            writer.write(judgeCommand.sourceCode());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return codePath;
    }

    /**
     * 주어진 파일 경로의 소스 코드를 지정된 박스에 복사합니다.
     * @param boxId 박스 ID
     * @param codePath 컴파일된 소스 코드 파일 경로
     */
    protected static void sendSourceCode(int boxId, String codePath) {
        final String makeDestDirCommand = "sudo mkdir -p " + getSandboxPath(boxId);
        final String sendSourceCodeCommand = "sudo cp " + codePath + " " + getSandboxPath(boxId);

        ProcessBuilder pb = new ProcessBuilder(
                "/bin/sh", "-c", makeDestDirCommand + " && " + sendSourceCodeCommand
        );

        try {
            Process process = pb.start();
            logging(process);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected static boolean checkAnswer(JudgeCommand command, int boxId, int testcaseNum) {
        final String answerFile = HOME_DIR + "/" + TESTCASE_PATH + command.problemId() + "/" + testcaseNum + ".out";
        final String outputFile = getSandboxPath(boxId) + testcaseNum + OUTPUT_FILE_SUFFIX;

        ProcessBuilder pb = new ProcessBuilder(
                "sudo",
                "diff", "-q", outputFile, answerFile
        );

        boolean isSolved = true;
        try {
            Process process = pb.start();
            logging(process);

            int exitCode = process.waitFor();
            if (exitCode == 1) isSolved = false;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return isSolved;
    }

    /**
     * 메타 파일을 읽어 테스트케이스 결과를 생성합니다.
     *
     * @param testcaseNum 테스트케이스 번호
     * @return TestcaseResult
     */
    protected static TestcaseResult readMetaFile(int testcaseNum) {
        final String metaFilePath = "meta/" + testcaseNum + META_FILE_SUFFIX;

        double runningTime = 0.0;
        int memoryUsage = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(metaFilePath))) {
            String line;
            String errorCode = "";
            String errorMessage = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("status:")) {
                    errorCode = line.split(":", 2)[1].trim();
                } else if (line.startsWith("message:")) {
                    errorMessage = line.split(":", 2)[1].trim();
                } else if (line.startsWith("time-wall:")) {
                    runningTime = Double.parseDouble(line.split(":", 2)[1].trim());
                } else if (line.startsWith("max-rss:")) {
                    memoryUsage = Integer.parseInt(line.split(":", 2)[1].trim());
                }
            }

            if (StringUtils.hasText(errorCode)) {
                return TestcaseResult.fail(testcaseNum, errorMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return TestcaseResult.success(testcaseNum, runningTime, memoryUsage);
    }

    protected static void cleanup(int boxId) {
        ProcessBuilder pb = new ProcessBuilder(
                "sudo",
                "isolate",
                "--box-id=" + boxId,
                "--cleanup"
        );

        try {
            Process process = pb.start();
            logging(process);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected static String getSandboxPath(int boxId) {
        return ISOLATE_ROOT + boxId + "/box/";
    }

    protected static void logging(Process process) throws IOException {
        BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String line;
        while ((line = stdOut.readLine()) != null) {
            log.debug(line);
        }
        while ((line = stdErr.readLine()) != null) {
            log.debug(line);
        }
    }
}
