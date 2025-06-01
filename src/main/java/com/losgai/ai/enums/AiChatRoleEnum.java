package com.losgai.ai.enums;

public enum AiChatRoleEnum {
    USER(0, "用户"),
    AI(1, "AI大模型"),
    LEAVE(2, "预留"),
    UNKNOWN(3,"未知");

    private final int code;
    private final String label;

    AiChatRoleEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static AiChatRoleEnum fromCode(Integer code) {
        if (code == null) return UNKNOWN;
        for (AiChatRoleEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN; // 找不到匹配的，返回 null
    }

    public static AiChatRoleEnum fromLabel(String label) {
        for (AiChatRoleEnum i : AiChatRoleEnum.values()) {
            if (i.label.equals(label)) {
                return i;
            }
        }
        throw new IllegalArgumentException("未知标签: " + label);
    }
}
