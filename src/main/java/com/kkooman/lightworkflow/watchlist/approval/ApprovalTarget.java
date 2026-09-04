package com.kkooman.lightworkflow.watchlist.approval;

public record ApprovalTarget(ApprovalTargetType type, String id) {
    public ApprovalTarget {
        if (type == null || id == null || id.isBlank()) {
            throw new IllegalArgumentException("결재 대상 유형과 식별자는 필수입니다.");
        }
    }
}
