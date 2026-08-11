package com.kkooman.lightworkflow.watchlist.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kkooman.lightworkflow.watchlist.api.WatchlistSearchRequest;
import com.kkooman.lightworkflow.watchlist.config.WatchlistSearchProperties;
import com.kkooman.lightworkflow.watchlist.domain.WatchlistEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WatchlistSearchServiceTest {
    private WatchlistSearchService service;

    @BeforeEach
    void setUp() {
        WatchlistSearchProperties properties = new WatchlistSearchProperties();
        properties.setFieldWeights(Map.of(
                "korean-name", 30F,
                "english-name", 25F,
                "date-of-birth", 15F,
                "country", 8F,
                "residence", 8F,
                "aka", 8F,
                "gender", 3F,
                "listing-reason", 3F));
        service = new WatchlistSearchService(properties);
        service.upsert(new WatchlistEntry(
                "wl-1", "홍길동", "Hong Gil Dong", "1980-01-02",
                "KR", "대한민국", List.of("Hong Gildong", "길동"), "M", "금융제재"));
        service.upsert(new WatchlistEntry(
                "wl-2", "김철수", "Kim Chul Soo", "1975-05-10",
                "US", "United States", List.of("Charles Kim"), "M", "사기"));
    }

    @Test
    void searchesKoreanNameWithTypo() {
        var results = service.search(new WatchlistSearchRequest("홍길돈", null, null, null, null, null, null, null));

        assertThat(results).extracting(result -> result.entry().id()).contains("wl-1");
        assertThat(results.getFirst().score()).isBetween(0D, 100D);
    }

    @Test
    void searchesEnglishNameAndAka() {
        var englishResults = service.search(new WatchlistSearchRequest(null, "Hong Gildong", null, null, null, null, null, null));
        var akaResults = service.search(new WatchlistSearchRequest(null, null, null, null, null, "Charles Kim", null, null));

        assertThat(englishResults).extracting(result -> result.entry().id()).contains("wl-1");
        assertThat(akaResults).extracting(result -> result.entry().id()).contains("wl-2");
    }

    @Test
    void searchesExactStructuredFieldsAndNormalizesTopScore() {
        var results = service.search(new WatchlistSearchRequest(null, null, "1980-01-02", "KR", null, null, null, null));

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().entry().id()).isEqualTo("wl-1");
        assertThat(results.getFirst().score()).isEqualTo(100D);
        assertThat(results).allMatch(result -> result.score() >= 0 && result.score() <= 100);
    }

    @Test
    void searchesResidenceGenderAndListingReason() {
        var residence = service.search(new WatchlistSearchRequest(null, null, null, null, "대한민국", null, null, null));
        var gender = service.search(new WatchlistSearchRequest(null, null, null, null, null, null, "M", null));
        var reason = service.search(new WatchlistSearchRequest(null, null, null, null, null, null, null, "금융제재"));

        assertThat(residence).extracting(result -> result.entry().id()).contains("wl-1");
        assertThat(gender).extracting(result -> result.entry().id()).containsExactlyInAnyOrder("wl-1", "wl-2");
        assertThat(reason).extracting(result -> result.entry().id()).contains("wl-1");
    }

    @Test
    void deletesAnIndexedEntry() {
        service.delete("wl-1");

        var results = service.search(new WatchlistSearchRequest("홍길동", null, null, null, null, null, null, null));

        assertThat(results).extracting(result -> result.entry().id()).doesNotContain("wl-1");
    }

    @Test
    void returnsNoResultsForEmptyRequest() {
        assertThat(service.search(new WatchlistSearchRequest(null, null, null, null, null, null, null, null))).isEmpty();
    }
}
