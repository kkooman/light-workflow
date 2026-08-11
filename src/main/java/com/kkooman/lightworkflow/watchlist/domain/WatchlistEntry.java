package com.kkooman.lightworkflow.watchlist.domain;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record WatchlistEntry(
        @NotBlank String id,
        String koreanName,
        String englishName,
        String dateOfBirth,
        String country,
        String residence,
        List<String> aka,
        String gender,
        String listingReason
) {
    public WatchlistEntry {
        aka = aka == null ? List.of() : List.copyOf(aka);
    }
}
