package com.kkooman.lightworkflow.watchlist.repository;

import com.kkooman.lightworkflow.watchlist.domain.WatchlistEntry;
import java.util.List;

public interface WatchlistEntryStore {
    int save(WatchlistEntry entry);

    int delete(String id);

    List<WatchlistEntry> findByIds(List<String> ids);

    List<WatchlistEntry> findAll();
}
