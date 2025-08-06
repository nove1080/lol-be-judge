package com.ssafy.c204_be_judge.judge.web.controller;

import com.ssafy.c204_be_judge.judge.service.JudgeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/judge/v1")
public class JudgeController {

    private final JudgeService judgeService;

    public JudgeController(@Qualifier("judgeServiceV2") JudgeService judgeService) {
        this.judgeService = judgeService;
    }


}
