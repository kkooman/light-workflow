package com.kkooman.lightworkflow.watchlist.api;

import java.util.List;

public record WatchlistSyncRequest(List<String> ids) {
    public WatchlistSyncRequest {
        ids = ids == null ? List.of() : List.copyOf(ids);
    }
}
