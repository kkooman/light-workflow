package com.kkooman.lightworkflow.watchlist.service;

import com.kkooman.lightworkflow.watchlist.api.WatchlistSearchRequest;
import com.kkooman.lightworkflow.watchlist.api.WatchlistSearchResult;
import com.kkooman.lightworkflow.watchlist.api.WatchlistRiskLevel;
import com.kkooman.lightworkflow.watchlist.api.WatchlistIndexStatus;
import com.kkooman.lightworkflow.watchlist.audit.WatchlistSearchAudit;
import com.kkooman.lightworkflow.watchlist.repository.WatchlistAuditStore;
import com.kkooman.lightworkflow.watchlist.config.WatchlistSearchProperties;
import com.kkooman.lightworkflow.watchlist.domain.WatchlistEntry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.OffsetDateTime;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WatchlistSearchService {
    private static final Logger log = LoggerFactory.getLogger(WatchlistSearchService.class);
    private static final List<String> FIELDS = List.of(
            "korean-name", "english-name", "date-of-birth", "country",
            "residence", "aka", "gender", "listing-reason");
    private static final List<String> ANALYZED_FIELDS = List.of("korean-name", "english-name", "aka", "listing-reason");

    private final Directory directory;
    private final Analyzer textAnalyzer = new PerFieldAnalyzerWrapper(
            new CJKAnalyzer(),
            Map.of("english-name", new StandardAnalyzer(), "aka", new StandardAnalyzer()));
    private final WatchlistSearchProperties properties;
    private final com.kkooman.lightworkflow.watchlist.repository.WatchlistEntryStore entryStore;
    private final WatchlistAuditStore auditStore;
    private volatile String indexState = "READY";
    private volatile long indexedDocumentCount;
    private volatile OffsetDateTime lastRebuiltAt;
    private volatile OffsetDateTime lastSyncedAt;

    @Autowired
    public WatchlistSearchService(
            WatchlistSearchProperties properties,
            com.kkooman.lightworkflow.watchlist.repository.WatchlistEntryStore entryStore) {
        this(properties, entryStore, null);
    }

    public WatchlistSearchService(
            WatchlistSearchProperties properties,
            com.kkooman.lightworkflow.watchlist.repository.WatchlistEntryStore entryStore,
            WatchlistAuditStore auditStore) {
        this.properties = properties;
        this.entryStore = entryStore;
        this.auditStore = auditStore;
            try {
                Files.createDirectories(Path.of(properties.getIndexPath()));
                this.directory = FSDirectory.open(Path.of(properties.getIndexPath()));
                try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(textAnalyzer))) {
                writer.commit();
                    indexedDocumentCount = writer.getDocStats().numDocs;
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Watchlist index could not be initialized", exception);
            }
    }

    public synchronized void upsert(WatchlistEntry entry) {
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(textAnalyzer))) {
            writer.updateDocument(new Term("id", entry.id()), toDocument(entry));
            writer.commit();
            entryStore.save(entry);
            indexedDocumentCount = countDocuments();
            lastSyncedAt = OffsetDateTime.now();
        } catch (IOException exception) {
            throw new IllegalStateException("Watchlist entry could not be indexed: " + entry.id(), exception);
        }
    }

    public synchronized void delete(String id) {
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(textAnalyzer))) {
            writer.deleteDocuments(new Term("id", id));
            writer.commit();
            entryStore.delete(id);
            indexedDocumentCount = countDocuments();
            lastSyncedAt = OffsetDateTime.now();
        } catch (IOException exception) {
            throw new IllegalStateException("Watchlist entry could not be deleted: " + id, exception);
        }
    }

    public synchronized int rebuild() {
        List<WatchlistEntry> entries = entryStore.findAll();
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(textAnalyzer))) {
            writer.deleteAll();
            for (WatchlistEntry entry : entries) {
                writer.addDocument(toDocument(entry));
            }
            writer.commit();
            indexedDocumentCount = entries.size();
            lastRebuiltAt = OffsetDateTime.now();
            lastSyncedAt = lastRebuiltAt;
            return entries.size();
        } catch (IOException exception) {
            throw new IllegalStateException("Watchlist index rebuild failed", exception);
        }
    }

    public synchronized int sync(List<String> ids) {
            List<WatchlistEntry> entries = entryStore.findByIds(ids);
            for (WatchlistEntry entry : entries) {
                indexOnly(entry);
            }
            lastSyncedAt = OffsetDateTime.now();
            return entries.size();
        }

    public WatchlistIndexStatus status() {
            return new WatchlistIndexStatus(indexState, indexedDocumentCount, lastRebuiltAt, lastSyncedAt);
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
            List<String> ids = new ArrayList<>();
            for (ScoreDoc hit : hits) {
                if (hit.score <= 0) {
                    continue;
                }
                String id = searcher.storedFields().document(hit.doc).get("id");
                ids.add(id);
            }
            Map<String, WatchlistEntry> entries = new java.util.HashMap<>();
            for (WatchlistEntry entry : entryStore.findByIds(ids)) {
                entries.put(entry.id(), entry);
            }
            for (ScoreDoc hit : hits) {
                if (hit.score > 0) {
                    String id = searcher.storedFields().document(hit.doc).get("id");
                    WatchlistEntry entry = entries.get(id);
                    if (entry != null) {
                        double score = Math.min(100, hit.score / highestScore * 100);
                        results.add(new WatchlistSearchResult(
                                entry, score, riskLevel(score), matchedFields(request, entry)));
                    }
                }
            }
            log.info("Watchlist search completed: requestedFields={}, candidateCount={}",
                    requestedFieldCount(request), results.size());
            saveAudit(request, results.size());
            return results.stream()
                    .sorted(Comparator.comparingDouble((WatchlistSearchResult result) -> result.score()).reversed())
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Watchlist search failed", exception);
        }
    }

    private void indexOnly(WatchlistEntry entry) {
            try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(textAnalyzer))) {
                writer.updateDocument(new Term("id", entry.id()), toDocument(entry));
                writer.commit();
                indexedDocumentCount = countDocuments();
            } catch (IOException exception) {
                throw new IllegalStateException("Watchlist entry could not be indexed: " + entry.id(), exception);
            }
        }

    private long countDocuments() {
            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                return reader.numDocs();
            } catch (IOException exception) {
                throw new IllegalStateException("Watchlist index status could not be read", exception);
            }
        }

    private void saveAudit(WatchlistSearchRequest request, int resultCount) {
            if (auditStore != null) {
                auditStore.save(new WatchlistSearchAudit(
                        OffsetDateTime.now(), requestedFieldCount(request), resultCount, hashRequest(request)));
            }
        }

    private String hashRequest(WatchlistSearchRequest request) {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                        .digest(String.join("|", request.values()).getBytes(StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                for (byte value : digest) {
                    result.append(String.format("%02x", value));
                }
                return result.toString();
            } catch (java.security.NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String riskLevel(double score) {
            if (score >= properties.getHighRiskThreshold()) {
                return WatchlistRiskLevel.HIGH.name();
            }
            if (score >= properties.getReviewThreshold()) {
                return WatchlistRiskLevel.REVIEW.name();
            }
            return WatchlistRiskLevel.LOW.name();
        }

    private List<String> matchedFields(WatchlistSearchRequest request, WatchlistEntry entry) {
            List<String> matched = new ArrayList<>();
            List<String> queries = request.values();
            List<String> values = List.of(
                    entry.koreanName(), entry.englishName(), entry.dateOfBirth(), entry.country(),
                    entry.residence(), String.join(" ", entry.aka()), entry.gender(), entry.listingReason());
            for (int index = 0; index < FIELDS.size(); index++) {
                String query = normalize(queries.get(index));
                String value = normalize(values.get(index) == null ? "" : values.get(index));
                if (!query.isBlank() && (value.contains(query) || query.contains(value)
                        || (ANALYZED_FIELDS.contains(FIELDS.get(index)) && editDistance(query, value) <= 2))) {
                    matched.add(FIELDS.get(index));
                }
            }
            return List.copyOf(matched);
        }

    private long requestedFieldCount(WatchlistSearchRequest request) {
            return request.values().stream().filter(value -> !value.isBlank()).count();
    }

    private int editDistance(String left, String right) {
            int[] previous = new int[right.length() + 1];
            for (int column = 0; column <= right.length(); column++) {
                previous[column] = column;
            }
            for (int row = 1; row <= left.length(); row++) {
                int[] current = new int[right.length() + 1];
                current[0] = row;
                for (int column = 1; column <= right.length(); column++) {
                    current[column] = Math.min(
                            Math.min(current[column - 1] + 1, previous[column] + 1),
                            previous[column - 1] + (left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1));
                }
                previous = current;
            }
            return previous[right.length()];
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
                fieldQuery = new TermQuery(new Term(field, normalized));
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
