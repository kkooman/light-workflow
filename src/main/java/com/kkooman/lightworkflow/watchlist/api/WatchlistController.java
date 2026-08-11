package com.kkooman.lightworkflow.watchlist.api;

import com.kkooman.lightworkflow.watchlist.domain.WatchlistEntry;
import com.kkooman.lightworkflow.watchlist.service.WatchlistSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Void> upsert(@Valid @RequestBody WatchlistEntry entry) {
        searchService.upsert(entry);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NotBlank String id) {
        searchService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public List<WatchlistSearchResult> search(@RequestBody WatchlistSearchRequest request) {
        return searchService.search(request);
    }

    @PostMapping("/rebuild")
    public ResponseEntity<Integer> rebuild() {
        return ResponseEntity.ok(searchService.rebuild());
    }
}
