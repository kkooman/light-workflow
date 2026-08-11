package com.kkooman.lightworkflow.watchlist.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WatchlistSearchProperties.class)
public class WatchlistSearchConfig {
}
