package com.kkooman.lightworkflow.watchlist.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kkooman.lightworkflow.watchlist.api.WatchlistSearchRequest;
import com.kkooman.lightworkflow.watchlist.config.WatchlistSearchProperties;
import com.kkooman.lightworkflow.watchlist.domain.WatchlistEntry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.kkooman.lightworkflow.watchlist.repository.WatchlistEntryStore;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WatchlistSearchServiceTest {
    private WatchlistSearchService service;
    private InMemoryWatchlistEntryStore store;
    private Path indexPath;

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
        indexPath = Path.of("build/test-index/" + System.nanoTime());
        properties.setIndexPath(indexPath.toString());
        store = new InMemoryWatchlistEntryStore();
        service = new WatchlistSearchService(properties, store);
        service.upsert(new WatchlistEntry(
                "wl-1", "홍길동", "Hong Gil Dong", "1980-01-02",
                "KR", "대한민국", List.of("Hong Gildong", "길동"), "M", "금융제재"));
        service.upsert(new WatchlistEntry(
                "wl-2", "김철수", "Kim Chul Soo", "1975-05-10",
                "US", "United States", List.of("Charles Kim"), "M", "사기"));
    }

    private static class InMemoryWatchlistEntryStore implements WatchlistEntryStore {
        private final Map<String, WatchlistEntry> entries = new ConcurrentHashMap<>();
        private int saveCalls;

        @Override
        public int save(WatchlistEntry entry) {
            entries.put(entry.id(), entry);
            saveCalls++;
            return 1;
        }

        @Override
        public int delete(String id) {
            return entries.remove(id) == null ? 0 : 1;
        }

        @Override
        public List<WatchlistEntry> findByIds(List<String> ids) {
            return ids.stream().map(entries::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public List<WatchlistEntry> findAll() {
            return List.copyOf(entries.values());
        }
    }

    @Test
    void searchesKoreanNameWithTypo() {
        var results = service.search(new WatchlistSearchRequest("홍길돈", null, null, null, null, null, null, null));

        assertThat(results).extracting(result -> result.entry().id()).contains("wl-1");
        assertThat(results.getFirst().score()).isBetween(0D, 100D);
    }

    @Test
    void ignoresWhitespaceOnlySearchValues() {
        var results = service.search(new WatchlistSearchRequest("  ", "\t", null, null, null, null, null, null));

        assertThat(results).isEmpty();
    }

    @Test
    void searchesEnglishNameAndAka() {
        var englishResults = service.search(new WatchlistSearchRequest(null, "Hong Gildong", null, null, null, null, null, null));
        var akaResults = service.search(new WatchlistSearchRequest(null, null, null, null, null, "Charles Kim", null, null));

        assertThat(englishResults).extracting(result -> result.entry().id()).contains("wl-1");
        assertThat(akaResults).extracting(result -> result.entry().id()).contains("wl-2");
    }

    @Test
    void normalizesCaseAndWhitespaceForEnglishSearch() {
        var results = service.search(new WatchlistSearchRequest(null, "  hOnG   gIl   DoNg ", null, null, null, null, null, null));

        assertThat(results).extracting(result -> result.entry().id()).contains("wl-1");
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
    void requiresStructuredFieldValueToMatchInsteadOfReturningUnrelatedRows() {
        var results = service.search(new WatchlistSearchRequest(null, null, "1980-01-03", null, null, null, null, null));

        assertThat(results).isEmpty();
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
    void ordersResultsByScoreAndKeepsScoresWithinBounds() {
        var results = service.search(new WatchlistSearchRequest("홍길동", null, null, "KR", null, null, null, null));

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().entry().id()).isEqualTo("wl-1");
        assertThat(results).extracting(result -> result.score())
                .allMatch(score -> Double.isFinite(score) && score >= 0D && score <= 100D)
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    void deletesAnIndexedEntry() {
        service.delete("wl-1");

        var results = service.search(new WatchlistSearchRequest("홍길동", null, null, null, null, null, null, null));

        assertThat(results).extracting(result -> result.entry().id()).doesNotContain("wl-1");
    }

    @Test
    void upsertReplacesExistingIndexDocumentAndDatabaseRow() {
        service.upsert(new WatchlistEntry(
                "wl-1", "박영희", "Younghee Park", "1980-01-02",
                "KR", "대한민국", List.of("Y. Park"), "F", "추가 심사"));

        assertThat(service.search(new WatchlistSearchRequest("홍길동", null, null, null, null, null, null, null))).isEmpty();
        assertThat(service.search(new WatchlistSearchRequest("박영희", null, null, null, null, null, null, null)))
                .extracting(result -> result.entry().id()).containsExactly("wl-1");
        assertThat(store.saveCalls).isEqualTo(3);
    }

    @Test
    void rebuildsIndexFromDatabaseStore() {
        service.delete("wl-1");
        assertThat(service.rebuild()).isEqualTo(1);

        var results = service.search(new WatchlistSearchRequest(null, "Kim Chul Soo", null, null, null, null, null, null));

        assertThat(results).extracting(result -> result.entry().id()).containsExactly("wl-2");
    }

    @Test
    void persistsIndexOnDiskAndCanBeOpenedByAnotherServiceInstance() {
        assertThat(Files.exists(indexPath)).isTrue();

        WatchlistSearchProperties properties = new WatchlistSearchProperties();
        properties.setIndexPath(indexPath.toString());
        WatchlistSearchService restartedService = new WatchlistSearchService(properties, store);

        var results = restartedService.search(new WatchlistSearchRequest(null, "Kim Chul Soo", null, null, null, null, null, null));
        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().entry().id()).isEqualTo("wl-2");
    }

    @Test
    void excludesIndexedRowsThatNoLongerExistInDatabase() {
        store.delete("wl-1");

        var results = service.search(new WatchlistSearchRequest("홍길동", null, null, null, null, null, null, null));

        assertThat(results).isEmpty();
    }

    @Test
    void returnsNoResultsForEmptyRequest() {
        assertThat(service.search(new WatchlistSearchRequest(null, null, null, null, null, null, null, null))).isEmpty();
    }
}
