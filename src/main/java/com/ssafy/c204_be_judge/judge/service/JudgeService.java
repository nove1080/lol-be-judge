package com.ssafy.c204_be_judge.judge.service;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.result.JudgeResult;

public interface JudgeService {

    JudgeResult run(JudgeCommand judgeCommand);

}
