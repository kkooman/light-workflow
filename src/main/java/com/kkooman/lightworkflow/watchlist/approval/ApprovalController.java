package com.kkooman.lightworkflow.watchlist.approval;

import com.kkooman.lightworkflow.api.ApiResponse;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {
    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/{approvalId}")
    public ApiResponse<ApprovalCase> find(@PathVariable String approvalId) {
        return ApiResponse.success(approvalService.find(approvalId), "결재 문서 조회 성공");
    }

    @GetMapping("/by-detection/{detectionId}")
    public ApiResponse<ApprovalCase> findByDetection(@PathVariable String detectionId) {
        return ApiResponse.success(approvalService.findByDetection(detectionId), "검출 결재 문서 조회 성공");
    }

    @PostMapping("/{approvalId}/submit")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('AML_SUBMITTER')")
    public ResponseEntity<ApiResponse<ApprovalCase>> submit(
            @PathVariable String approvalId, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                approvalService.approve(approvalId, principal.getName(), ApprovalStep.SUBMITTER),
                "상신자 결재 완료"));
    }

    @PostMapping("/{approvalId}/aml-officer/approve")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('AML_OFFICER')")
    public ResponseEntity<ApiResponse<ApprovalCase>> approveByOfficer(
            @PathVariable String approvalId, Principal principal,
            @RequestHeader(value = "X-Department", required = false) String department) {
        return ResponseEntity.ok(ApiResponse.success(
                approvalService.approve(approvalId, principal.getName(), department, ApprovalStep.AML_OFFICER),
                "AML담당자 결재 완료"));
    }

    @PostMapping("/{approvalId}/aml-manager/approve")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('AML_MANAGER')")
    public ResponseEntity<ApiResponse<ApprovalCase>> approveByManager(
            @PathVariable String approvalId, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                approvalService.approve(approvalId, principal.getName(), ApprovalStep.AML_MANAGER),
                "AML책임자 결재 완료"));
    }
}
