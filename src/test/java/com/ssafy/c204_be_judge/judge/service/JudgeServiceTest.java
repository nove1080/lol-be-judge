package com.ssafy.c204_be_judge.judge.service;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
class JudgeServiceTest {
    //todo 컴파일 실패 테스트 코드 작성
    
    private static JudgeCommand getWrongJudgeCommand(Long problemId) {
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
                                 if (target < 0) {
                                     System.out.println("Target cannot be negative");
                                     return;
                                 }
                                  
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
                .timeLimit(0.5)
                .memoryLimit(256)
                .build();
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