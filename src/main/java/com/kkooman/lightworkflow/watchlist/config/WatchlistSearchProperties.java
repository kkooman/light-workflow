package com.kkooman.lightworkflow.watchlist.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watchlist.search")
public class WatchlistSearchProperties {
    private Map<String, Float> fieldWeights = new LinkedHashMap<>();
    private String indexPath = "./data/lucene/watchlist";
    private double highRiskThreshold = 90;
    private double reviewThreshold = 70;

    public Map<String, Float> getFieldWeights() {
        return fieldWeights;
    }

    public void setFieldWeights(Map<String, Float> fieldWeights) {
        this.fieldWeights = new LinkedHashMap<>(fieldWeights);
    }

    public String getIndexPath() {
        return indexPath;
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }

    public double getHighRiskThreshold() {
        return highRiskThreshold;
    }

    public void setHighRiskThreshold(double highRiskThreshold) {
        this.highRiskThreshold = highRiskThreshold;
    }

    public double getReviewThreshold() {
        return reviewThreshold;
    }

    public void setReviewThreshold(double reviewThreshold) {
        this.reviewThreshold = reviewThreshold;
    }
}
