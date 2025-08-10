package com.ssafy.c204_be_judge.judge.worker;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.domain.JudgeResult;
import com.ssafy.c204_be_judge.judge.domain.TestcaseResult;
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
 * WSL 환경에서 채점을 수행하는 JudgeWorker 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeWorkerForWsl implements JudgeWorker {

    private static final String META_FILE_SUFFIX = "_meta.txt";

    private static final String ISOLATE_ROOT = "/var/lib/isolate/";
    private static final Integer THREAD_POOL_SIZE = 2;

    private static final String TESTCASE_ORIGIN_PATH = "/mnt/c/Users/SSAFY/testcases/";
    private static final String TESTCASE_MOUNT_PATH = "/mnt/c/Users/SSAFY/testcases/";

    public JudgeResult run(JudgeCommand judgeCommand) {
        String codePath = writeSourceCode(judgeCommand);
        if (!compile(judgeCommand)) {
            return JudgeResult.fail(judgeCommand, "컴파일에 실패하였습니다.");
        }

        int totalTestcaseCount = getTotalTestcaseCount(judgeCommand);
        List<TestcaseResult> testcaseResults = new ArrayList<>();

        try {
            prepareJudge(judgeCommand, codePath);
            for (int start = 1; start <= totalTestcaseCount; start += THREAD_POOL_SIZE) {
                int end = Math.min(start + THREAD_POOL_SIZE - 1, totalTestcaseCount);
                testcaseResults.addAll(runTestcases(judgeCommand, start, end));
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return JudgeResult.fail(judgeCommand, "채점 중 오류가 발생하였습니다.");
        }

        return JudgeResult.success(judgeCommand, testcaseResults);
    }

    /**
     * 주어진 범위의 테스트케이스를 병렬로 실행합니다. <br>
     * 병렬처리 크기는 {@value THREAD_POOL_SIZE}로 제한됩니다.
     * @param judgeCommand 채점 명령
     * @param start 시작 테스트케이스 번호
     * @param end 종료 테스트케이스 번호
     * @return 테스트케이스 결과 리스트
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private static List<TestcaseResult> runTestcases(JudgeCommand judgeCommand, int start, int end) throws InterruptedException, ExecutionException {
        return new ForkJoinPool(THREAD_POOL_SIZE).submit(() ->
                IntStream.rangeClosed(start, end)
                        .parallel()
                        .mapToObj(testcase -> {
                            int boxId = testcase % THREAD_POOL_SIZE + 1;
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

    private static void prepareJudge(JudgeCommand judgeCommand, String codePath) throws InterruptedException, ExecutionException {
        new ForkJoinPool(THREAD_POOL_SIZE).submit(() ->
                IntStream.rangeClosed(1, THREAD_POOL_SIZE).parallel()
                        .forEach(i -> {
                                    prepareSandbox(i);
                                    sendSourceCode(judgeCommand, i, codePath);
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
                "wsl", "-d", "ubuntu", "-u", "root", "--",
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
        final String baseDir = System.getProperty("user.home");
        final String filePath = baseDir + "/sourceCode/" + judgeCommand.problemId() + "/Main.java"; //todo 배포 환경으로 맞게 변경

        File file = new File(filePath);
        file.getParentFile().mkdirs();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            writer.write(judgeCommand.sourceCode());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return filePath;
    }

    private static void sendSourceCode(JudgeCommand judgeCommand, int boxId, String filePath) {
        filePath = "/mnt/c/Users/SSAFY/sourceCode/" + judgeCommand.problemId() + "/Main.class"; //todo 제거
        final String destPath = ISOLATE_ROOT + boxId + "/box/";

        final String makeDestDirCommand = "mkdir -p " + destPath;
        final String sendSourceCodeCommand = "cp " + filePath + " " + destPath;
        final String command = makeDestDirCommand + " && " + sendSourceCodeCommand;

        ProcessBuilder pb = new ProcessBuilder(
                "wsl", "-d", "ubuntu", "-u", "root", "--",
                "/bin/sh", "-c", command
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
        final String baseDir = System.getProperty("user.home");
        final String dirPath = baseDir + "/testcases/" + judgeCommand.problemId(); //todo 배포 환경으로 맞게 변경

        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("디렉토리가 유효하지 않습니다! [경로: %s]".formatted(dirPath));
        }

        return dir.list((f, name) -> name.contains(".in")).length;
    }

    private static boolean compile(JudgeCommand judgeCommand) {
        final String baseDir = System.getProperty("user.home");
        final String filePath = baseDir + "/sourceCode/" + judgeCommand.problemId() + "/Main.java"; //todo 배포 환경으로 맞게 변경

        ProcessBuilder pb = new ProcessBuilder(
                "javac", "-J-Xms1024m", "-J-Xmx1920m", "-J-Xss512m", "-encoding", "UTF-8", filePath
        );

        try {
            Process process = pb.start();
            logging(process);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private static String executeSourceCode(JudgeCommand judgeCommand, int boxId, int testcaseNum) {
        String testcasePath = TESTCASE_ORIGIN_PATH + judgeCommand.problemId();
        String filePath = "/mnt/c/Users/SSAFY/sourceCode/" + judgeCommand.problemId(); //todo 제거

        ProcessBuilder pb = new ProcessBuilder(
                "wsl", "-d", "ubuntu", "-u", "root", "--",
                "isolate",
                "--box-id=" + boxId,
                "--processes=128",
                "--dir=/usr/lib/jvm",
                "--dir=/etc/java-17-openjdk/security",
                "--dir=" + filePath,
                "--dir=" + testcasePath,
                "--stdin=" + testcasePath + "/" + testcaseNum + ".in",
                "--stdout=" + testcaseNum + "_output.txt",
                "--stderr=" + testcaseNum + "_err.txt",
                "--time=" + judgeCommand.timeLimit(),
                "--mem=" + 16384000, //16GB
                "--meta=" + testcaseNum + META_FILE_SUFFIX,
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
        final String boxRoot = ISOLATE_ROOT + boxId + "/box/";
        final String answerFile = TESTCASE_MOUNT_PATH + command.problemId() + "/" + testcaseNum + ".out";
        final String outputFile = boxRoot + testcaseNum + "_output.txt";

        ProcessBuilder pb = new ProcessBuilder(
                "wsl", "-d", "ubuntu", "-u", "root", "--",
                "diff", "-q", outputFile, answerFile
        );

        boolean isCorrect = true;
        try {
            Process process = pb.start();
            logging(process);

            int exitCode = process.waitFor();
            if (exitCode == 1) isCorrect = false;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return isCorrect;
    }

    /**
     * 메타 파일을 읽어 테스트케이스 결과를 생성합니다.
     * @param testcaseNum 테스트케이스 번호
     * @return TestcaseResult
     */
    private static TestcaseResult readMetaFile(int testcaseNum) {
        final String metaFilePath = + testcaseNum + META_FILE_SUFFIX; //todo 제거

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
                "wsl", "-d", "ubuntu", "-u", "root", "--",
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
