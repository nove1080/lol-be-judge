package com.ssafy.c204_be_judge.judge.worker;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.domain.JudgeResult;
import com.ssafy.c204_be_judge.judge.domain.TestcaseResult;
import com.ssafy.c204_be_judge.validation.exception.CompileException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/**
 * Ubuntu 환경에서 채점을 수행하는 JudgeWorker 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeWorkerForUbuntu implements JudgeWorker {

    private static final int MAX_THREAD_POOL_SIZE = Math.max(1, Runtime.getRuntime().availableProcessors());

    private static final String HOME_DIR = System.getProperty("user.home"); // /home/ubuntu
    private static final String ISOLATE_ROOT = "/var/lib/isolate/";
    private static final String TESTCASE_PATH = "testcases/";

    private static final String OUTPUT_FILE_SUFFIX = "_output.txt";
    private static final String ERROR_FILE_SUFFIX = "_err.txt";
    private static final String META_FILE_SUFFIX = "_meta.txt";

    @PostConstruct
    public void init() {
        log.info("JudgeWorkerV3 initialized with THREAD_POOL_SIZE={}", MAX_THREAD_POOL_SIZE);
    }

    public JudgeResult run(JudgeCommand judgeCommand) {
        List<TestcaseResult> testcaseResults = new ArrayList<>();

        try {
            String javaFilePath = writeSourceCode(judgeCommand);
            String classFilePath = compile(javaFilePath);
            int totalTestcaseCount = getTotalTestcaseCount(judgeCommand);

            prepareJudge(classFilePath);

            for (int start = 1; start <= totalTestcaseCount; start += MAX_THREAD_POOL_SIZE) {
                int end = Math.min(start + MAX_THREAD_POOL_SIZE - 1, totalTestcaseCount);
                testcaseResults.addAll(runTestcases(judgeCommand, start, end));
            }
        } catch (CompileException e) {
            return JudgeResult.fail(judgeCommand, "컴파일에 실패하였습니다.");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return JudgeResult.fail(judgeCommand, "채점 중 오류가 발생하였습니다.");
        }

        return JudgeResult.success(judgeCommand, testcaseResults);
    }

    /**
     * 주어진 범위의 테스트케이스를 병렬로 실행합니다. <br>
     * 병렬처리 크기는 {@link #MAX_THREAD_POOL_SIZE}로 제한됩니다.
     * @param judgeCommand 채점 명령
     * @param start 시작 테스트케이스 번호
     * @param end 종료 테스트케이스 번호
     * @return 테스트케이스 결과 리스트
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private static List<TestcaseResult> runTestcases(JudgeCommand judgeCommand, int start, int end) throws InterruptedException, ExecutionException {
        return new ForkJoinPool(MAX_THREAD_POOL_SIZE).submit(() ->
                IntStream.rangeClosed(start, end)
                        .parallel()
                        .mapToObj(testcase -> {
                            int boxId = testcase % MAX_THREAD_POOL_SIZE + 1;
                            boolean isSolved = runTestcase(judgeCommand, boxId, testcase);

                            if (isSolved) {
                                return readMetaFile(testcase);
                            } else {
                                return TestcaseResult.fail(testcase);
                            }
                        })
                        .toList()
        ).get();
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

    private static boolean runTestcase(JudgeCommand judgeCommand, int boxId, int testcase) {
        executeSourceCode(judgeCommand, boxId, testcase);
        return checkAnswer(judgeCommand, boxId, testcase);
    }

    // 초기화 작업
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

    private static String writeSourceCode(JudgeCommand judgeCommand) {
        final String codePath = HOME_DIR + "/sourceCode/" + judgeCommand.problemId() + "/Main.java";

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

    private static int getTotalTestcaseCount(JudgeCommand judgeCommand) {
        final String testcasePath = HOME_DIR + "/testcases/" + judgeCommand.problemId();

        File dir = new File(testcasePath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("디렉토리가 유효하지 않습니다! [경로: %s]".formatted(testcasePath));
        }

        return dir.list((f, name) -> name.contains(".in")).length;
    }

    /**
     * 주어진 Java 파일을 컴파일합니다.
     * @param javaFilePath 컴파일할 Java 파일의 경로
     * @return 컴파일된 클래스 파일의 경로
     * @throws CompileException 컴파일 실패 시 예외 발생
     */
    private static String compile(String javaFilePath) throws CompileException {
        ProcessBuilder pb = new ProcessBuilder(
                "javac", "-J-Xms1024m", "-J-Xmx1920m", "-J-Xss512m", "-encoding", "UTF-8", javaFilePath
        );

        try {
            Process process = pb.start();
            logging(process);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new CompileException("컴파일에 실패하였습니다. [경로: %s]".formatted(javaFilePath));
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new CompileException("컴파일에 실패하였습니다. [경로: %s]".formatted(javaFilePath), e);
        }

        return javaFilePath.replaceAll("\\.java$", ".class");
    }

    private static String executeSourceCode(JudgeCommand judgeCommand, int boxId, int testcaseNum) {
        final String testcasePath = HOME_DIR + "/" + TESTCASE_PATH + judgeCommand.problemId();

        ProcessBuilder pb = new ProcessBuilder(
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
                "--time=" + judgeCommand.timeLimit(),
                "--mem=" + 16384000, //16GB
                "--meta=" + "meta/" + testcaseNum + META_FILE_SUFFIX,
                "--run",
                "--",
                "/usr/lib/jvm/java-17-openjdk-amd64/bin/java",
                "-Xms1024m", "-Xmx1920m", "-Xss512m", "-Dfile.encoding=UTF-8", "-XX:+UseSerialGC",
                "Main"
        );

        try {
            Process process = pb.start();
            logging(process);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return "";
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

    /**
     * 메타 파일을 읽어 테스트케이스 결과를 생성합니다.
     * @param testcaseNum 테스트케이스 번호
     * @return TestcaseResult
     */
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
