package com.ssafy.c204_be_judge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
public class C204BeJudgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(C204BeJudgeApplication.class, args);
	}

}
