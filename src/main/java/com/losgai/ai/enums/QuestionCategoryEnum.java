package com.losgai.ai.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum QuestionCategoryEnum {
    EASY_SELECT(0),     // 0-简单选择题
    MEDIUM_SELECT(1),   // 1-中等选择题
    HARD_SELECT(2),     // 2-困难选择题
    EASY_JUDGE(3),      // 3-简单判断题
    MEDIUM_JUDGE(4),    // 4-中等判断题
    HARD_JUDGE(5),      // 5-困难判断题
    EASY_SHORT(6),      // 6-简单简答题
    MEDIUM_SHORT(7),    // 7-中等简答题
    HARD_SHORT(8);      // 8-困难简答题

    private final int code;

    QuestionCategoryEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    private static final Map<Integer, QuestionCategoryEnum> MAP = Arrays.stream(values())
        .collect(Collectors.toMap(QuestionCategoryEnum::getCode, e -> e));

    public static QuestionCategoryEnum fromCode(int code) {
        return MAP.get(code);
    }
}