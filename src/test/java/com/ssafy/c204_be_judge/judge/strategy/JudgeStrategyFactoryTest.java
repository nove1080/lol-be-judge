package com.ssafy.c204_be_judge.judge.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssafy.c204_be_judge.judge.domain.ProgrammingLanguage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JudgeStrategyFactoryTest {

    private JudgeStrategyFactory judgeStrategyFactory;

    @BeforeEach
    void setUp() {
        List<JudgeStrategy> strategies = List.of(
            new JavaJudgeStrategy(),
            new CppJudgeStrategy(),
            new PythonJudgeStrategy()
        );
        judgeStrategyFactory = new JudgeStrategyFactory(strategies);
    }

    @Test
    @DisplayName("Java 언어로 전달 시 JavaJudgeStrategy를 반환한다.")
    void findStrategy_java() {
        Optional<JudgeStrategy> strategy = judgeStrategyFactory.findStrategy("Java");

        assertThat(strategy).isPresent();
        assertThat(strategy.get()).isInstanceOf(JavaJudgeStrategy.class);
        assertThat(strategy.get().getLanguage()).isEqualTo(ProgrammingLanguage.JAVA);
    }

    @Test
    @DisplayName("C++ 언어로 전달 시 CppJudgeStrategy를 반환한다.")
    void findStrategy_cpp() {
        Optional<JudgeStrategy> strategy = judgeStrategyFactory.findStrategy("C++");

        assertThat(strategy).isPresent();
        assertThat(strategy.get()).isInstanceOf(CppJudgeStrategy.class);
        assertThat(strategy.get().getLanguage()).isEqualTo(ProgrammingLanguage.CPP);
    }

    @Test
    @DisplayName("Python 언어로 전달 시 PythonJudgeStrategy를 반환한다.")
    void findStrategy_python() {
        Optional<JudgeStrategy> strategy = judgeStrategyFactory.findStrategy("Python");

        assertThat(strategy).isPresent();
        assertThat(strategy.get()).isInstanceOf(PythonJudgeStrategy.class);
        assertThat(strategy.get().getLanguage()).isEqualTo(ProgrammingLanguage.PYTHON);
    }

    @Test
    @DisplayName("지원하지 않는 언어로 전달 시 빈 Optional을 반환한다.")
    void findStrategy_unsupported() {
        Optional<JudgeStrategy> strategy = judgeStrategyFactory.findStrategy("Rust");

        assertThat(strategy).isEmpty();
    }
}
