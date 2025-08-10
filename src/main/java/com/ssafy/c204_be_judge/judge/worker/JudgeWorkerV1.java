package com.ssafy.c204_be_judge.judge.worker;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;

import java.io.*;

import com.ssafy.c204_be_judge.judge.domain.JudgeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * [채점 과정]
 * 1. 샌드박스 준비
 * 2. 호스트 서버에 소스코드 작성
 * 3. 샌드박스로 소스코드 전송
 * 5. 샌드박스로 테스트케이스 전송
 * 6. 샌드박스에서 컴파일
 * 7. 테스트케이스 실행
 * 8. 정답 확인
 *
 * 특징: 모든 테스트케이스의 실행을 순차처리 합니다.
 */
@Slf4j
@Component
@Deprecated(since = "v2로 변경됨")
public class JudgeWorkerV1 implements JudgeWorker {

    private static final String OUTPUT_FILE = "output.txt";
    private static final String ISOLATE_ROOT = "/var/lib/isolate/";

    public JudgeResult run(JudgeCommand judgeCommand) {
        prepareSandbox(judgeCommand);
        sendSourceCode(judgeCommand, writeSourceCode(judgeCommand));

        int totalTestcaseCount = getTotalTestcaseCount(judgeCommand);
        sendTestcase(judgeCommand);
        compile(judgeCommand);

        for (int i = 1; i < totalTestcaseCount + 1; i++) {
            executeSourceCode(judgeCommand, i);
            checkAnswer(judgeCommand, i);
        }

        return null;
    }

    private static void prepareSandbox(JudgeCommand judgeCommand) {
        ProcessBuilder pb = new ProcessBuilder(
                "wsl", "-d", "ubuntu", "-u", "root", "--",
                "isolate",
                "--box-id=" + judgeCommand.problemId(),
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

    private static void sendSourceCode(JudgeCommand judgeCommand, String filePath) {
        filePath = "/mnt/c/Users/SSAFY/sourceCode/" + judgeCommand.problemId() + "/Main.java"; //todo 제거
        final String destPath = ISOLATE_ROOT + judgeCommand.problemId() + "/box/";

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

    private static void sendTestcase(JudgeCommand judgeCommand) {
        final String originPath = "/mnt/c/Users/SSAFY/testcases/" + judgeCommand.problemId(); //todo 배포 환경으로 맞게 변경
        final String destPath = ISOLATE_ROOT + judgeCommand.problemId() + "/box/testcases/";

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
        ProcessBuilder pb = new ProcessBuilder(
                "wsl", "-d", "ubuntu", "-u", "root", "--",
                "isolate",
                "--box-id=" + judgeCommand.problemId(),
                "--processes=128",
                "--dir=/usr/lib/jvm",
                "--dir=/etc/java-17-openjdk/security",
                "--run",
                "--",
                "/usr/lib/jvm/java-17-openjdk-amd64/bin/javac",
                "-encoding", "UTF-8",
                "Main.java"
        );

        try {
            Process process = pb.start();
            logging(process);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static String executeSourceCode(JudgeCommand judgeCommand, int testcaseNum) {
        ProcessBuilder pb = new ProcessBuilder(
                "wsl", "-d", "ubuntu", "-u", "root", "--",
                "isolate",
                "--box-id=" + judgeCommand.problemId(),
                "--processes=128",
                "--dir=/usr/lib/jvm",
                "--dir=/etc/java-17-openjdk/security",
                "--stdin=testcases/" + judgeCommand.problemId() + "/" + testcaseNum + ".in",
                "--stdout=" + testcaseNum + "_output.txt",
                "--stderr=err.txt",
                "--time=" + judgeCommand.timeLimit(),
                "--mem=" + 4096000, //4GB
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

    private static boolean checkAnswer(JudgeCommand command, int testcaseNum) {
        final String boxRoot = ISOLATE_ROOT + command.problemId() + "/box/";
        final String answerFile = boxRoot + "testcases" + "/" + command.problemId() + "/" + testcaseNum + ".out";
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
//        System.out.println(testcaseNum + "번 결과: " + (isCorrect ? "정답" : "오답"));
        return isCorrect;
    }

    private static void cleanup(JudgeCommand judgeCommand) {
        ProcessBuilder pb = new ProcessBuilder(
                "wsl", "-d", "ubuntu", "-u", "root", "--",
                "isolate",
                "--box-id=" + judgeCommand.problemId(),
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
