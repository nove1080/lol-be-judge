package com.ssafy.c204_be_judge.judge.worker;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.domain.JudgeResult;
import com.ssafy.c204_be_judge.judge.domain.TestcaseResult;
import com.ssafy.c204_be_judge.judge.strategy.JudgeStrategy;
import com.ssafy.c204_be_judge.validation.exception.CompileException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class JudgeWorker {

    private static final int MAX_THREAD_POOL_SIZE = Math.max(1, Runtime.getRuntime().availableProcessors());

    private static final String HOME_DIR = JudgeStrategy.HOME_DIR;
    private static final String ISOLATE_ROOT = JudgeStrategy.ISOLATE_ROOT;
    private static final String TESTCASE_PATH = JudgeStrategy.TESTCASE_PATH;

    private static final String OUTPUT_FILE_SUFFIX = JudgeStrategy.OUTPUT_FILE_SUFFIX;
    private static final String ERROR_FILE_SUFFIX = JudgeStrategy.ERROR_FILE_SUFFIX;
    private static final String META_FILE_SUFFIX = JudgeStrategy.META_FILE_SUFFIX;

    public JudgeResult run(JudgeCommand judgeCommand, JudgeStrategy strategy) {
        StopWatch stopWatch = new StopWatch();
        List<TestcaseResult> testcaseResults = new ArrayList<>();

        try {
            stopWatch.start("1. write source code");
            String filePath = writeSourceCode(judgeCommand, strategy.getFileName());
            stopWatch.stop();

            stopWatch.start("2. compile");
            String classFilePath = compile(filePath, strategy);
            stopWatch.stop();

            int totalTestcaseCount = getTotalTestcaseCount(judgeCommand);

            stopWatch.start("3. prepare sandbox & send source code to box");
            prepareJudge(classFilePath);
            stopWatch.stop();

            stopWatch.start("4. run testcases");
            for (int start = 1; start <= totalTestcaseCount; start += MAX_THREAD_POOL_SIZE) {
                int end = Math.min(start + MAX_THREAD_POOL_SIZE - 1, totalTestcaseCount);
                testcaseResults.addAll(runTestcases(judgeCommand, strategy, start, end));
            }
            stopWatch.stop();

            log.info("채점 과정별 소요 시간: {}", stopWatch.prettyPrint());
            log.info("채점 완료! 소요 시간: {}초", stopWatch.getTotalTimeSeconds());
        } catch (CompileException e) {
            return JudgeResult.fail(judgeCommand, "컴파일에 실패하였습니다.");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return JudgeResult.fail(judgeCommand, "채점 중 오류가 발생하였습니다.");
        }

        return JudgeResult.success(judgeCommand, testcaseResults);
    }

    private String compile(String filePath, JudgeStrategy strategy) throws CompileException {
        ProcessBuilder pb = new ProcessBuilder(strategy.getCompileCommand(filePath));

        try {
            Process process = pb.start();
            logging(process);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new CompileException("컴파일에 실패하였습니다. [경로: %s]".formatted(filePath));
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new CompileException("컴파일에 실패하였습니다. [경로: %s]".formatted(filePath), e);
        }

        return strategy.getCompiledFilePath(filePath);
    }

    private static void prepareJudge(String codePath) throws InterruptedException, ExecutionException {
        new ForkJoinPool(MAX_THREAD_POOL_SIZE).submit(() ->
            IntStream.rangeClosed(1, MAX_THREAD_POOL_SIZE).parallel()
                .forEach(threadNumber -> {
                        prepareSandbox(threadNumber);
                        sendSourceCode(threadNumber, codePath);
                    }
                )
        ).get();
    }

    private List<TestcaseResult> runTestcases(JudgeCommand judgeCommand, JudgeStrategy strategy, int start, int end) throws InterruptedException, ExecutionException {
        return new ForkJoinPool(MAX_THREAD_POOL_SIZE).submit(() ->
            IntStream.rangeClosed(start, end)
                .parallel()
                .mapToObj(testcase -> {
                    int boxId = testcase % MAX_THREAD_POOL_SIZE + 1;
                    boolean isSolved = runTestcase(judgeCommand, strategy, boxId, testcase);

                    if (isSolved) {
                        return readMetaFile(testcase);
                    } else {
                        return TestcaseResult.fail(testcase);
                    }
                })
                .toList()
        ).get();
    }

    private boolean runTestcase(JudgeCommand judgeCommand, JudgeStrategy strategy, int boxId, int testcase) {
        executeSourceCode(judgeCommand, strategy, boxId, testcase);
        return checkAnswer(judgeCommand, boxId, testcase);
    }

    private static void prepareSandbox(int boxId) {
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

    private static int getTotalTestcaseCount(JudgeCommand judgeCommand) {
        final String testcasePath = HOME_DIR + "/testcases/" + judgeCommand.problemId();

        File dir = new File(testcasePath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("디렉토리가 유효하지 않습니다! [경로: %s]".formatted(testcasePath));
        }

        return dir.list((f, name) -> name.contains(".in")).length;
    }

    private static String writeSourceCode(JudgeCommand judgeCommand, String fileName) {
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

    private static void sendSourceCode(int boxId, String codePath) {
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

    private static boolean checkAnswer(JudgeCommand command, int boxId, int testcaseNum) {
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

    private static TestcaseResult readMetaFile(int testcaseNum) {
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

    private String executeSourceCode(JudgeCommand judgeCommand, JudgeStrategy strategy, int boxId, int testcaseNum) {
        ProcessBuilder pb = new ProcessBuilder(strategy.getRunCommand(judgeCommand, boxId, testcaseNum));

        try {
            Process process = pb.start();
            logging(process);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return "";
    }

    private static void cleanup(int boxId) {
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

    private static String getSandboxPath(int boxId) {
        return ISOLATE_ROOT + boxId + "/box/";
    }

    private static void logging(Process process) throws IOException {
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
