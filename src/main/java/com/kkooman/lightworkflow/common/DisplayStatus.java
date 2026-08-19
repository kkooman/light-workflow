package com.kkooman.lightworkflow.common;

public enum DisplayStatus implements CommonCode {
    ACTIVE("활성"),
    INACTIVE("비활성"),
    DELETED("삭제");

    private final String label;

    DisplayStatus(String label) {
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
