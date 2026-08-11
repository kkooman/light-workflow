package com.kkooman.lightworkflow.watchlist.api;

import java.time.OffsetDateTime;

public record WatchlistIndexStatus(
        String state,
        long indexedDocumentCount,
        OffsetDateTime lastRebuiltAt,
        OffsetDateTime lastSyncedAt
) {
}
