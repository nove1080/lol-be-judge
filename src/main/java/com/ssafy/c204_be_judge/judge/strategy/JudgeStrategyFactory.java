package com.ssafy.c204_be_judge.judge.strategy;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JudgeStrategyFactory {

    private final List<JudgeStrategy> strategies;

    public Optional<JudgeStrategy> findStrategy(String programmingLanguage) {
        if (programmingLanguage == null) {
            return Optional.empty();
        }
        return strategies.stream()
                .filter(strategy -> strategy.supports(programmingLanguage))
                .findFirst();
    }
}
