package com.ssafy.c204_be_judge.judge.service;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JudgeServiceTest {

    @Autowired private JudgeService judgeService;

    @Test
    @DisplayName("")
    void example() {
        JudgeCommand judgeCommand = getJudgeCommand(1L);
        judgeService.run(judgeCommand);
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
                .timeLimit(0.09)
                .memoryLimit(256)
                .build();
    }

}