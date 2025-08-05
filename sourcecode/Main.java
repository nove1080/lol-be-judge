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