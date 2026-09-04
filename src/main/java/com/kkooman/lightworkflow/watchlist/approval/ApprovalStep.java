package com.kkooman.lightworkflow.watchlist.approval;

public enum ApprovalStep {
    SUBMITTER("상신자"),
    AML_OFFICER("AML담당자"),
    AML_MANAGER("AML책임자"),
    COMPLETED("결재완료");

    private final String label;

    ApprovalStep(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
