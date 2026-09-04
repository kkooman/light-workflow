package com.kkooman.lightworkflow.watchlist.api;

import com.kkooman.lightworkflow.watchlist.domain.WatchlistEntry;
import com.kkooman.lightworkflow.watchlist.service.WatchlistSearchService;
import com.kkooman.lightworkflow.watchlist.approval.ApprovalService;
import com.kkooman.lightworkflow.watchlist.detection.WatchlistDetectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {
    private final WatchlistSearchService searchService;
    private final WatchlistDetectionService detectionService;
    private final ApprovalService approvalService;

    public WatchlistController(WatchlistSearchService searchService) {
        this(searchService, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public WatchlistController(WatchlistSearchService searchService,
            WatchlistDetectionService detectionService, ApprovalService approvalService) {
        this.searchService = searchService;
        this.detectionService = detectionService;
        this.approvalService = approvalService;
    }

    @PostMapping("/entries")
    public ResponseEntity<com.kkooman.lightworkflow.api.ApiResponse<Void>> upsert(@Valid @RequestBody WatchlistEntry entry) {
        searchService.upsert(entry);
        return ResponseEntity.ok(com.kkooman.lightworkflow.api.ApiResponse.success(null, "위험도 리스트 항목 저장 성공"));
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<com.kkooman.lightworkflow.api.ApiResponse<Void>> delete(@PathVariable @NotBlank String id) {
        searchService.delete(id);
        return ResponseEntity.ok(com.kkooman.lightworkflow.api.ApiResponse.success(null, "위험도 리스트 항목 삭제 성공"));
    }

    @PostMapping("/search")
    public com.kkooman.lightworkflow.api.ApiResponse<List<WatchlistSearchResult>> search(
            @RequestBody WatchlistSearchRequest request, Principal principal) {
        List<WatchlistSearchResult> results = searchService.search(request);
        if (detectionService != null && approvalService != null) {
            String submittedBy = principal == null ? "system" : principal.getName();
            results.forEach(result -> {
                var detection = detectionService.register(result, submittedBy);
                approvalService.submit(detection.detectionId(), submittedBy);
            });
        }
        return com.kkooman.lightworkflow.api.ApiResponse.success(results, "검색 성공");
    }

    public com.kkooman.lightworkflow.api.ApiResponse<List<WatchlistSearchResult>> search(
            WatchlistSearchRequest request) {
        return search(request, null);
    }

    @PostMapping("/rebuild")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<com.kkooman.lightworkflow.api.ApiResponse<Integer>> rebuild() {
        return ResponseEntity.ok(com.kkooman.lightworkflow.api.ApiResponse.success(searchService.rebuild(), "인덱스 재구성 완료"));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<com.kkooman.lightworkflow.api.ApiResponse<Integer>> sync(@RequestBody WatchlistSyncRequest request) {
        return ResponseEntity.ok(com.kkooman.lightworkflow.api.ApiResponse.success(searchService.sync(request.ids()), "인덱스 동기화 완료"));
    }

    @org.springframework.web.bind.annotation.GetMapping("/index/status")
    public com.kkooman.lightworkflow.api.ApiResponse<WatchlistIndexStatus> indexStatus() {
        return com.kkooman.lightworkflow.api.ApiResponse.success(searchService.status(), "인덱스 상태 조회 성공");
    }
}
