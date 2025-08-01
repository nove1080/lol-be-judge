package com.ssafy.c204_be_judge.judge.service;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.*;

class JudgeServiceTest {

    @Test
    void processBuilderExample() throws IOException {
        ProcessBuilder pb = new ProcessBuilder();

        pb.command("cmd.exe", "/c", ".\\test.bat");
//        builder.command("sh", "-c", "/home/test.sh");

        Process process = pb.start();

        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line = null;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }

        if (process != null) {
            process.destroy();
        }
    }

}