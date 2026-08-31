package com.kkooman.lightworkflow.watchlist.api;

import com.kkooman.lightworkflow.watchlist.domain.WatchlistEntry;
import com.kkooman.lightworkflow.watchlist.service.WatchlistSearchService;
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

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {
    private final WatchlistSearchService searchService;

    public WatchlistController(WatchlistSearchService searchService) {
        this.searchService = searchService;
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
    public com.kkooman.lightworkflow.api.ApiResponse<List<WatchlistSearchResult>> search(@RequestBody WatchlistSearchRequest request) {
        return com.kkooman.lightworkflow.api.ApiResponse.success(searchService.search(request), "검색 성공");
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
