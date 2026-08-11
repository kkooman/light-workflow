package com.kkooman.lightworkflow.watchlist.api;

import com.kkooman.lightworkflow.watchlist.domain.WatchlistEntry;

import java.util.List;

public record WatchlistSearchResult(
        WatchlistEntry entry,
        double score,
        String riskLevel,
        List<String> matchedFields
) {
}
