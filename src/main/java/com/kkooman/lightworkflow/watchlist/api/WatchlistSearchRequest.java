package com.kkooman.lightworkflow.watchlist.api;

public record WatchlistSearchRequest(
        String koreanName,
        String englishName,
        String dateOfBirth,
        String country,
        String residence,
        String aka,
        String gender,
        String listingReason
) {
    public boolean isEmpty() {
        return values().stream().allMatch(value -> value == null || value.isBlank());
    }

    public java.util.List<String> values() {
        return java.util.stream.Stream.of(koreanName, englishName, dateOfBirth, country, residence, aka, gender, listingReason)
                .map(value -> value == null ? "" : value)
                .toList();
    }
}
