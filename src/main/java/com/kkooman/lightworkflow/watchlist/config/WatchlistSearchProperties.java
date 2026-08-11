package com.kkooman.lightworkflow.watchlist.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watchlist.search")
public class WatchlistSearchProperties {
    private Map<String, Float> fieldWeights = new LinkedHashMap<>();

    public Map<String, Float> getFieldWeights() {
        return fieldWeights;
    }

    public void setFieldWeights(Map<String, Float> fieldWeights) {
        this.fieldWeights = new LinkedHashMap<>(fieldWeights);
    }
}
