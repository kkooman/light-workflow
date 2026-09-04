package com.kkooman.lightworkflow.watchlist.approval;

import java.time.OffsetDateTime;

public record ApprovalCase(
        String approvalId,
        String detectionId,
        String submittedBy,
        ApprovalTarget amlOfficer,
        ApprovalStep currentStep,
        OffsetDateTime submittedAt) {
}
