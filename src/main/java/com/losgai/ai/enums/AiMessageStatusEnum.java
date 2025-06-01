package com.losgai.ai.enums;

public enum AiMessageStatusEnum {
    GENERATING(0, "生产中"),
    FINISHED(1, "已经完成"),
    STOPPED(2, "已经中断"),
    UNKNOWN(3,"未知");

    private final int code;
    private final String label;

    AiMessageStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static AiMessageStatusEnum fromCode(Integer code) {
        if (code == null) return UNKNOWN;
        for (AiMessageStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN; // 找不到匹配的，返回 null
    }

    public static AiMessageStatusEnum fromLabel(String label) {
        for (AiMessageStatusEnum i : AiMessageStatusEnum.values()) {
            if (i.label.equals(label)) {
                return i;
            }
        }
        throw new IllegalArgumentException("未知标签: " + label);
    }
}
