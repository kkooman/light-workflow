package com.kkooman.lightworkflow.watchlist.detection;

import com.kkooman.lightworkflow.watchlist.api.WatchlistSearchResult;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class WatchlistDetectionService {
    private final WatchlistDetectionStore detectionStore;

    public WatchlistDetectionService(WatchlistDetectionStore detectionStore) {
        this.detectionStore = detectionStore;
    }

    public WatchlistDetection register(WatchlistSearchResult result, String submittedBy) {
        WatchlistDetection detection = new WatchlistDetection(
                UUID.randomUUID().toString(),
                result.entry().id(),
                result.entry().id(),
                result.score(),
                result.riskLevel(),
                result.matchedFields(),
                submittedBy,
                OffsetDateTime.now());
        detectionStore.save(detection);
        return detection;
    }
}
