package com.kkooman.lightworkflow.watchlist.audit;

import java.time.OffsetDateTime;

public record WatchlistSearchAudit(
        OffsetDateTime searchedAt,
        long requestedFieldCount,
        int resultCount,
        String requestHash
) {
}
