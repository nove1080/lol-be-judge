package com.ssafy.c204_be_judge.judge.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProgrammingLanguage {
    JAVA("Java"),
    CPP("C++"),
    PYTHON("Python");

    private final String displayName;

    public static boolean isJava(String name) {
        return JAVA.getDisplayName().equalsIgnoreCase(name);
    }

    public static boolean isCpp(String name) {
        return CPP.getDisplayName().equalsIgnoreCase(name);
    }

    public static boolean isPython(String name) {
        return PYTHON.getDisplayName().equalsIgnoreCase(name);
    }
}
