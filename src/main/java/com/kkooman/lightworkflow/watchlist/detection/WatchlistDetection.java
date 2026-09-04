package com.kkooman.lightworkflow.watchlist.detection;

import java.time.OffsetDateTime;
import java.util.List;

public record WatchlistDetection(
        String detectionId,
        String customerId,
        String watchlistEntryId,
        double score,
        String riskLevel,
        List<String> matchedFields,
        String submittedBy,
        OffsetDateTime detectedAt) {

    public WatchlistDetection {
        matchedFields = matchedFields == null ? List.of() : List.copyOf(matchedFields);
    }
}
