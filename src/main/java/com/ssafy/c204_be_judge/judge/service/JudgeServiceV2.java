package com.ssafy.c204_be_judge.judge.service;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.result.JudgeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

@Service
@Slf4j
public class JudgeServiceV2 {

    private static final String OUTPUT_FILE = "output.txt";
    private static final String ISOLATE_ROOT = "/var/lib/isolate/";
    private static final List<Integer> boxIds = List.of(1, 2, 3, 4, 5, 6, 7, 8);

    public JudgeResult run(JudgeCommand judgeCommand) {
        String codePath = writeSourceCode(judgeCommand);
        compile(judgeCommand);

        int totalTestcaseCount = getTotalTestcaseCount(judgeCommand);

        ForkJoinPool forkJoinPool = new ForkJoinPool(15);

        try {
            forkJoinPool
                    .submit(() -> IntStream.range(1, totalTestcaseCount + 1)
                            .parallel()
                            .forEach(testcase -> {
                                prepareSandbox(testcase);
                                sendSourceCode(judgeCommand, testcase, codePath);
                                sendTestcase(judgeCommand, testcase, testcase);
                                executeSourceCode(judgeCommand, testcase, testcase);
                                checkAnswer(judgeCommand, testcase, testcase);
                            })
                    )
                    .get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

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

    private static void sendTestcase(JudgeCommand judgeCommand, int boxId, int testcaseNum) {
        final String originPath = "/mnt/c/Users/SSAFY/testcases/" + judgeCommand.problemId() + "/" + testcaseNum + ".*"; //todo 배포 환경으로 맞게 변경
        final String destPath = ISOLATE_ROOT + boxId + "/box/testcases";

        final String makeDestDirCommand = "mkdir -p " + destPath;
        final String copyTestcaseCommand = "cp -r " + originPath + " " + destPath;
        final String command = makeDestDirCommand + " && " + copyTestcaseCommand;

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

    private static void compile(JudgeCommand judgeCommand) {
        final String baseDir = System.getProperty("user.home");
        final String filePath = baseDir + "/sourceCode/" + judgeCommand.problemId() + "/Main.java"; //todo 배포 환경으로 맞게 변경

        ProcessBuilder pb = new ProcessBuilder(
                "javac", filePath
        );

        try {
            Process process = pb.start();
            logging(process);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static String executeSourceCode(JudgeCommand judgeCommand, int boxId, int testcaseNum) {
        ProcessBuilder pb = new ProcessBuilder(
                "wsl", "-d", "ubuntu", "-u", "root", "--",
                "isolate",
                "--box-id=" + boxId,
                "--processes=128",
                "--dir=/usr/lib/jvm",
                "--dir=/etc/java-17-openjdk/security",
                "--stdin=testcases/" + testcaseNum + ".in",
                "--stdout=" + testcaseNum + "_output.txt",
                "--stderr=" + testcaseNum + "_err.txt",
                "--time=" + judgeCommand.timeLimit(),
                "--mem=" + 40960000, //4GB
                "--run",
                "--",
                "/usr/lib/jvm/java-17-openjdk-amd64/bin/java",
                "-Xmx" + judgeCommand.memoryLimit() + "m", "-cp", ".", "Main"
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
        final String answerFile = boxRoot + "testcases/" + testcaseNum + ".out";
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
        log.debug(testcaseNum + "번 결과: " + (isCorrect ? "정답" : "오답"));
        return isCorrect;
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
