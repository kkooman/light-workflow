package com.kkooman.lightworkflow.watchlist.repository;

import com.kkooman.lightworkflow.watchlist.audit.WatchlistSearchAudit;

public interface WatchlistAuditStore {
    int save(WatchlistSearchAudit audit);
}
