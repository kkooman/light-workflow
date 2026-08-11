package com.kkooman.lightworkflow.watchlist.service;

import com.kkooman.lightworkflow.watchlist.api.WatchlistSearchRequest;
import com.kkooman.lightworkflow.watchlist.api.WatchlistSearchResult;
import com.kkooman.lightworkflow.watchlist.config.WatchlistSearchProperties;
import com.kkooman.lightworkflow.watchlist.domain.WatchlistEntry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;

@Service
public class WatchlistSearchService {
    private static final List<String> FIELDS = List.of(
            "korean-name", "english-name", "date-of-birth", "country",
            "residence", "aka", "gender", "listing-reason");
    private static final List<String> ANALYZED_FIELDS = List.of("korean-name", "english-name", "aka", "listing-reason");

    private final Directory directory = new ByteBuffersDirectory();
    private final Analyzer textAnalyzer = new PerFieldAnalyzerWrapper(
            new CJKAnalyzer(),
            Map.of("english-name", new StandardAnalyzer(), "aka", new StandardAnalyzer()));
    private final WatchlistSearchProperties properties;
    private final Map<String, WatchlistEntry> entries = new ConcurrentHashMap<>();

    public WatchlistSearchService(WatchlistSearchProperties properties) {
        this.properties = properties;
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(textAnalyzer))) {
            writer.commit();
        } catch (IOException exception) {
            throw new IllegalStateException("Watchlist index could not be initialized", exception);
        }
    }

    public synchronized void upsert(WatchlistEntry entry) {
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(textAnalyzer))) {
            writer.updateDocument(new Term("id", entry.id()), toDocument(entry));
            writer.commit();
            entries.put(entry.id(), entry);
        } catch (IOException exception) {
            throw new IllegalStateException("Watchlist entry could not be indexed: " + entry.id(), exception);
        }
    }

    public synchronized void delete(String id) {
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(textAnalyzer))) {
            writer.deleteDocuments(new Term("id", id));
            writer.commit();
            entries.remove(id);
        } catch (IOException exception) {
            throw new IllegalStateException("Watchlist entry could not be deleted: " + id, exception);
        }
    }

    public List<WatchlistSearchResult> search(WatchlistSearchRequest request) {
        if (request.isEmpty()) {
            return List.of();
        }
        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = buildQuery(request);
            ScoreDoc[] hits = searcher.search(query, 100).scoreDocs;
            float highestScore = hits.length == 0 || hits[0].score <= 0 ? 1 : hits[0].score;
            List<WatchlistSearchResult> results = new ArrayList<>();
            for (ScoreDoc hit : hits) {
                if (hit.score <= 0) {
                    continue;
                }
                String id = searcher.storedFields().document(hit.doc).get("id");
                WatchlistEntry entry = entries.get(id);
                if (entry != null) {
                    results.add(new WatchlistSearchResult(entry, Math.min(100, hit.score / highestScore * 100)));
                }
            }
            return results.stream()
                    .sorted(Comparator.comparingDouble((WatchlistSearchResult result) -> result.score()).reversed())
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Watchlist search failed", exception);
        }
    }

    private Query buildQuery(WatchlistSearchRequest request) {
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        List<String> values = request.values();
        for (int index = 0; index < FIELDS.size(); index++) {
            String value = values.get(index).trim();
            if (value.isBlank()) {
                continue;
            }
            String field = FIELDS.get(index);
            String normalized = normalize(value);
            Query fieldQuery;
            if (ANALYZED_FIELDS.contains(field)) {
                fieldQuery = analyzedFuzzyQuery(field, normalized);
            } else {
                fieldQuery = new FuzzyQuery(new Term(field, normalized), 1);
            }
            float weight = properties.getFieldWeights().getOrDefault(field, 1F);
            query.add(new BoostQuery(fieldQuery, weight), BooleanClause.Occur.SHOULD);
        }
        return query.build();
    }

    private Document toDocument(WatchlistEntry entry) {
        Document document = new Document();
        document.add(new StringField("id", entry.id(), Field.Store.YES));
        addAnalyzed(document, "korean-name", entry.koreanName());
        addAnalyzed(document, "english-name", entry.englishName());
        addKeyword(document, "date-of-birth", entry.dateOfBirth());
        addKeyword(document, "country", entry.country());
        addKeyword(document, "residence", entry.residence());
        entry.aka().forEach(value -> addAnalyzed(document, "aka", value));
        addKeyword(document, "gender", entry.gender());
        addAnalyzed(document, "listing-reason", entry.listingReason());
        return document;
    }

    private void addAnalyzed(Document document, String field, String value) {
        if (value != null && !value.isBlank()) {
            String normalized = normalize(value);
            document.add(new TextField(field, normalized, Field.Store.NO));
            document.add(new StringField(field + "-exact", normalized, Field.Store.NO));
        }
    }

    private Query analyzedFuzzyQuery(String field, String value) {
        BooleanQuery.Builder tokens = new BooleanQuery.Builder();
        Term exactTerm = new Term(field + "-exact", value);
        tokens.add(new TermQuery(exactTerm), BooleanClause.Occur.SHOULD);
        tokens.add(new FuzzyQuery(exactTerm, 2), BooleanClause.Occur.SHOULD);
        try (var stream = textAnalyzer.tokenStream(field, value)) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                Term tokenTerm = new Term(field, term.toString());
                tokens.add(new TermQuery(tokenTerm), BooleanClause.Occur.SHOULD);
                tokens.add(new FuzzyQuery(tokenTerm, 2), BooleanClause.Occur.SHOULD);
            }
            stream.end();
        } catch (IOException exception) {
            throw new IllegalStateException("Search query could not be analyzed", exception);
        }
        return tokens.build();
    }

    private void addKeyword(Document document, String field, String value) {
        if (value != null && !value.isBlank()) {
            document.add(new StringField(field, normalize(value), Field.Store.NO));
        }
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
