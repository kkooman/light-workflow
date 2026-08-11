package com.kkooman.lightworkflow.watchlist.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WatchlistSearchRequestTest {
    @Test
    void preservesFieldOrderAndNormalizesNullsToEmptyValues() {
        WatchlistSearchRequest request = new WatchlistSearchRequest(
                "홍길동", null, "1980-01-02", null, null, "길동", "M", null);

        assertThat(request.values()).containsExactly(
                "홍길동", "", "1980-01-02", "", "", "길동", "M", "");
        assertThat(request.isEmpty()).isFalse();
    }

    @Test
    void recognizesNullAndBlankRequestsAsEmpty() {
        assertThat(new WatchlistSearchRequest(null, null, null, null, null, null, null, null).isEmpty()).isTrue();
        assertThat(new WatchlistSearchRequest(" ", "\t", "", null, null, null, null, null).isEmpty()).isTrue();
    }
}
