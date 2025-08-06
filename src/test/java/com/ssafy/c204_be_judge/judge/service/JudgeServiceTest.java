package com.ssafy.c204_be_judge.judge.service;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;

@SpringBootTest
@ActiveProfiles("test")
class JudgeServiceTest {

    @Autowired
    @Qualifier("judgeServiceV2")
    private JudgeService judgeService;

    @Test
    @DisplayName("채점서비스 v2 테스트")
    void judgeServiceV2() {
        System.out.println("사용 가능한 스레드 : " + Runtime.getRuntime().availableProcessors());
        JudgeCommand judgeCommand = getJudgeCommand(1L);
        judgeService.run(judgeCommand);
    }

    @Test
    void parallel() throws InterruptedException {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        System.out.println("사용 가능한 스레드 : " + availableProcessors);

        long s = System.currentTimeMillis();
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        list.parallelStream()
                .forEach(i -> {
                    try {
                        System.out.println("현재 숫자 : " + i + " 스레드 이름 : " + Thread.currentThread().getName());
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
        long e = System.currentTimeMillis();
        System.out.println("총 걸린 시간 : " + (e - s) + "ms");
    }

    private static JudgeCommand getJudgeCommand(Long problemId) {
        final String sourceCode =
                """
                        import java.io.BufferedReader;
                        import java.io.InputStreamReader;
                        import java.util.*;
                        
                        public class Main {
                        
                            static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                            static long[] nums;
                            static long target;
                            static String answer = "None";
                        
                            public static void main(String[] args) throws Exception {
                                init();
                        
                                Map<Long, Integer> map = new HashMap<>();
                                for (int i = 0; i < nums.length; i++) {
                                    if (map.containsKey(target - nums[i])) {
                                        answer = map.get(target - nums[i]) + " " + i;
                                        break;
                                    }
                        
                                    map.put(nums[i], i);
                                }
                        
                                System.out.println(answer);
                            }
                        
                            public static void init() throws Exception {
                                String[] input = br.readLine().split(" ");
                        
                                nums = new long[input.length];
                                for (int i = 0; i < input.length; i++) {
                                    nums[i] = Long.parseLong(input[i]);
                                }
                        
                                target = Long.parseLong(br.readLine());
                            }
                        }
                        """;
        return JudgeCommand.builder()
                .problemId(problemId)
                .sourceCode(sourceCode)
                .programmingLanguage("JAVA")
                .timeLimit(1.0)
                .memoryLimit(256)
                .build();
    }

}