package com.kkooman.lightworkflow.watchlist.repository;

import com.kkooman.lightworkflow.watchlist.domain.WatchlistEntry;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WatchlistEntryMapper extends WatchlistEntryStore {
    @Override
    int save(WatchlistEntry entry);

    @Override
    int delete(String id);

    @Override
    List<WatchlistEntry> findByIds(List<String> ids);

    @Override
    List<WatchlistEntry> findAll();
}
