package com.ssafy.c204_be_judge.judge.worker;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.domain.JudgeResult;
import com.ssafy.c204_be_judge.judge.domain.TestcaseResult;
import com.ssafy.c204_be_judge.validation.exception.CompileException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
public class JudgeWorkerForCpp extends JudgeWorker {

    private static final String CPP_FILE_NAME = "Main.cpp";

    @PostConstruct
    public void init() {
        log.debug("JudgeWorkerForCpp initialized with THREAD_POOL_SIZE={}", MAX_THREAD_POOL_SIZE);
    }

    public JudgeResult run(JudgeCommand judgeCommand) {
        List<TestcaseResult> testcaseResults = new ArrayList<>();

        try {
            String cppFilePath = writeSourceCode(judgeCommand, CPP_FILE_NAME);
            String classFilePath = compile(cppFilePath);
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
    private List<TestcaseResult> runTestcases(JudgeCommand judgeCommand, int start, int end) throws InterruptedException, ExecutionException {
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

    private boolean runTestcase(JudgeCommand judgeCommand, int boxId, int testcase) {
        executeSourceCode(judgeCommand, boxId, testcase);
        return checkAnswer(judgeCommand, boxId, testcase);
    }

    /**
     * 주어진 python 파일을 컴파일합니다.
     * @param filePath 컴파일할 python 파일의 경로
     * @return 컴파일된 클래스 파일의 경로
     * @throws CompileException 컴파일 실패 시 예외 발생
     */
    protected String compile(String filePath) throws CompileException {
        String compiledFilePath = filePath.replaceAll("\\.cpp$", "");
        ProcessBuilder pb = new ProcessBuilder(
                "g++", filePath, "-o", compiledFilePath, "-O2", "-Wall", "-lm", "-static", "-std=gnu++17"
        );

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

        return compiledFilePath;
    }

    protected String executeSourceCode(JudgeCommand judgeCommand, int boxId, int testcaseNum) {
        final String testcasePath = HOME_DIR + "/" + TESTCASE_PATH + judgeCommand.problemId();

        ProcessBuilder pb = new ProcessBuilder(
                "sudo",
                "isolate",
                "--box-id=" + boxId,
                "--processes=128",
                "--dir=" + testcasePath,
                "--stdin=" + testcasePath + "/" + testcaseNum + ".in",
                "--stdout=" + testcaseNum + OUTPUT_FILE_SUFFIX,
                "--stderr=" + testcaseNum + ERROR_FILE_SUFFIX,
                "--time=" + judgeCommand.timeLimit(),
                "--mem=" + 16384000, //16GB
                "--meta=" + "meta/" + testcaseNum + META_FILE_SUFFIX,
                "--run",
                "--",
                "./Main"
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

}
