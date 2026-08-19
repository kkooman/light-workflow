package com.kkooman.lightworkflow.common;

public enum UserType implements CommonCode {
    ADMIN("관리자"),
    USER("일반 사용자"),
    GUEST("게스트");

    private final String label;

    UserType(String label) {
        this.label = label;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String label() {
        return label;
    }
}
