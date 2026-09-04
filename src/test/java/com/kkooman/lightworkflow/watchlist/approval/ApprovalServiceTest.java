package com.kkooman.lightworkflow.watchlist.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ApprovalServiceTest {

    @Test
    void approvalMovesFromSubmitterToOfficerToManagerInOrder() {
        ApprovalService service = new ApprovalService();
        ApprovalCase submitted = service.submit("detection-1", "submitter");

        ApprovalCase officerStep = service.approve(
                submitted.approvalId(), "submitter", ApprovalStep.SUBMITTER);
        ApprovalCase managerStep = service.approve(
                submitted.approvalId(), "officer", "AML", ApprovalStep.AML_OFFICER);
        ApprovalCase completed = service.approve(
                submitted.approvalId(), "manager", ApprovalStep.AML_MANAGER);

        assertThat(officerStep.currentStep()).isEqualTo(ApprovalStep.AML_OFFICER);
        assertThat(managerStep.currentStep()).isEqualTo(ApprovalStep.AML_MANAGER);
        assertThat(completed.currentStep()).isEqualTo(ApprovalStep.COMPLETED);
    }

    @Test
    void onlyTheSubmitterCanApproveSubmitterStep() {
        ApprovalService service = new ApprovalService();
        ApprovalCase submitted = service.submit("detection-1", "submitter");

        assertThatThrownBy(() -> service.approve(
                submitted.approvalId(), "other-user", ApprovalStep.SUBMITTER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cannotSkipApprovalStep() {
        ApprovalService service = new ApprovalService();
        ApprovalCase submitted = service.submit("detection-1", "submitter");

        assertThatThrownBy(() -> service.approve(
                submitted.approvalId(), "officer", ApprovalStep.AML_OFFICER))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void departmentCanApproveWhenAssignedAsAmlOfficer() {
        ApprovalService service = new ApprovalService();
        ApprovalCase submitted = service.submit(
                "detection-1", "submitter",
                new ApprovalTarget(ApprovalTargetType.DEPARTMENT, "AML-KYC"));

        service.approve(submitted.approvalId(), "submitter", ApprovalStep.SUBMITTER);

        ApprovalCase approved = service.approve(
                submitted.approvalId(), "department-user", "AML-KYC", ApprovalStep.AML_OFFICER);

        assertThat(approved.currentStep()).isEqualTo(ApprovalStep.AML_MANAGER);
    }

    @Test
    void personCanApproveWhenAssignedAsAmlOfficer() {
        ApprovalService service = new ApprovalService();
        ApprovalCase submitted = service.submit(
                "detection-1", "submitter",
                new ApprovalTarget(ApprovalTargetType.PERSON, "officer"));

        service.approve(submitted.approvalId(), "submitter", ApprovalStep.SUBMITTER);

        assertThat(service.approve(
                submitted.approvalId(), "officer", ApprovalStep.AML_OFFICER).currentStep())
                .isEqualTo(ApprovalStep.AML_MANAGER);
    }
}
