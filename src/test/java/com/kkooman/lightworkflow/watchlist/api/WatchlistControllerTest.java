package com.kkooman.lightworkflow.watchlist.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kkooman.lightworkflow.watchlist.domain.WatchlistEntry;
import com.kkooman.lightworkflow.watchlist.service.WatchlistSearchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class WatchlistControllerTest {
    private WatchlistSearchService service;
    private WatchlistController controller;

    @BeforeEach
    void setUp() {
        service = mock(WatchlistSearchService.class);
        controller = new WatchlistController(service);
    }

    @Test
    void delegatesEntryLifecycleAndReturnsExpectedStatuses() {
        WatchlistEntry entry = new WatchlistEntry(
                "wl-1", "홍길동", "Hong Gil Dong", null, null, null, List.of(), null, null);

        assertThat(controller.upsert(entry).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.delete("wl-1").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(service).upsert(entry);
        verify(service).delete("wl-1");
    }

    @Test
    void delegatesSearchRebuildSyncAndStatus() {
        WatchlistSearchRequest request = new WatchlistSearchRequest("홍길동", null, null, null, null, null, null, null);
        WatchlistSearchResult result = new WatchlistSearchResult(
                new WatchlistEntry("wl-1", "홍길동", null, null, null, null, List.of(), null, null),
                100D, "HIGH", List.of("korean-name"));
        WatchlistIndexStatus status = new WatchlistIndexStatus("READY", 1, null, null);
        when(service.search(request)).thenReturn(List.of(result));
        when(service.rebuild()).thenReturn(1);
        when(service.sync(List.of("wl-1"))).thenReturn(1);
        when(service.status()).thenReturn(status);

        assertThat(controller.search(request)).containsExactly(result);
        assertThat(controller.rebuild().getBody()).isEqualTo(1);
        assertThat(controller.sync(new WatchlistSyncRequest(List.of("wl-1"))).getBody()).isEqualTo(1);
        assertThat(controller.indexStatus()).isEqualTo(status);

        verify(service).search(request);
        verify(service).rebuild();
        verify(service).sync(List.of("wl-1"));
        verify(service).status();
    }
}
