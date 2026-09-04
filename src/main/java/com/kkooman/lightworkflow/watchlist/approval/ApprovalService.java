package com.kkooman.lightworkflow.watchlist.approval;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ApprovalService {
    private final Map<String, ApprovalCase> cases = new ConcurrentHashMap<>();

    public ApprovalCase submit(String detectionId, String submittedBy) {
        return submit(detectionId, submittedBy,
                new ApprovalTarget(ApprovalTargetType.DEPARTMENT, "AML"));
    }

    public ApprovalCase submit(String detectionId, String submittedBy, ApprovalTarget amlOfficer) {
        ApprovalCase approvalCase = new ApprovalCase(
                UUID.randomUUID().toString(),
                detectionId,
                submittedBy,
                amlOfficer,
                ApprovalStep.SUBMITTER,
                OffsetDateTime.now());
        cases.put(approvalCase.approvalId(), approvalCase);
        return approvalCase;
    }

    public ApprovalCase approve(String approvalId, String username, ApprovalStep step) {
        return approve(approvalId, username, null, step);
    }

    public ApprovalCase approve(String approvalId, String username, String department, ApprovalStep step) {
        ApprovalCase current = find(approvalId);
        if (current.currentStep() != step) {
            throw new IllegalStateException("결재 순서가 올바르지 않습니다. 현재 단계: " + current.currentStep().label());
        }
        if (step == ApprovalStep.SUBMITTER && !current.submittedBy().equals(username)) {
            throw new AccessDeniedException("상신자만 상신 결재를 처리할 수 있습니다.");
        }
        if (step == ApprovalStep.AML_OFFICER && !isAssignedOfficer(current.amlOfficer(), username, department)) {
            throw new AccessDeniedException("지정된 AML담당자 또는 소속 부서만 결재할 수 있습니다.");
        }
        ApprovalStep next = switch (step) {
            case SUBMITTER -> ApprovalStep.AML_OFFICER;
            case AML_OFFICER -> ApprovalStep.AML_MANAGER;
            case AML_MANAGER -> ApprovalStep.COMPLETED;
            case COMPLETED -> throw new IllegalStateException("이미 결재가 완료되었습니다.");
        };
        ApprovalCase updated = new ApprovalCase(
                current.approvalId(), current.detectionId(), current.submittedBy(),
                current.amlOfficer(), next, current.submittedAt());
        cases.put(approvalId, updated);
        return updated;
    }

    private boolean isAssignedOfficer(ApprovalTarget target, String username, String department) {
        return target.type() == ApprovalTargetType.PERSON
                ? target.id().equals(username)
                : target.id().equalsIgnoreCase(department);
    }

    public ApprovalCase find(String approvalId) {
        ApprovalCase approvalCase = cases.get(approvalId);
        if (approvalCase == null) {
            throw new IllegalArgumentException("결재 문서를 찾을 수 없습니다: " + approvalId);
        }
        return approvalCase;
    }

    public ApprovalCase findByDetection(String detectionId) {
        return cases.values().stream()
                .filter(approvalCase -> approvalCase.detectionId().equals(detectionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("검출 결재 문서를 찾을 수 없습니다: " + detectionId));
    }
}
