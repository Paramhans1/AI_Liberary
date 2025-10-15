package com.ai.library.controller;

import com.ai.library.service.SemanticSearchService;
import com.ai.library.service.SemanticSearchService.SearchResult;
import com.ai.library.ai.OpenAiClient;
import com.ai.library.model.Book;
import com.ai.library.repository.BookRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/semantic")
public class SemanticSearchController {
    private final SemanticSearchService service;
    // optional compatibility fields used by tests that instantiate this controller directly
    private final OpenAiClient openAiClient;
    private final BookRepository bookRepository;

    @Autowired
    public SemanticSearchController(SemanticSearchService service) {
        this.service = service;
        this.openAiClient = null;
        this.bookRepository = null;
    }

    // Compatibility constructor used in tests: SemanticSearchTest instantiates controller with (OpenAiClient, BookRepository)
    public SemanticSearchController(OpenAiClient openAiClient, BookRepository bookRepository) {
        this.service = null;
        this.openAiClient = openAiClient;
        this.bookRepository = bookRepository;
    }

    @PostMapping("/ingest/book/{bookId}")
    public ResponseEntity<?> ingestBook(@PathVariable Long bookId) {
        var chunks = service.ingestBook(bookId);
        return ResponseEntity.ok(java.util.Map.of("ingested", chunks.size()));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q, @RequestParam(defaultValue = "5") int top) {
        List<SearchResult> res = service.search(q, top);
        var out = res.stream().map(r -> java.util.Map.of(
                "chunkId", r.chunk.getId(),
                "bookId", r.chunk.getBookId(),
                "content", r.chunk.getContent(),
                "score", r.score
        )).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    // Non-REST helper used by existing unit test which directly calls semanticSearch
    public ResponseEntity<?> semanticSearch(String q, int top) {
        if (openAiClient == null || bookRepository == null) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", "not configured"));
        }
        double[] qEmb = openAiClient.generateEmbedding(q);
        List<Book> all = bookRepository.findAll();
        List<java.util.Map<String, Object>> ranked = all.stream().map(b -> {
            double[] emb = parseEmbedding(b.getEmbeddingJson());
            double score = cosineSimilarity(qEmb, emb);
            return java.util.Map.of("book", b, "score", score);
        }).sorted((m1, m2) -> Double.compare((double) m2.get("score"), (double) m1.get("score")))
                .limit(top)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ranked);
    }

    private double[] parseEmbedding(String json) {
        if (json == null) return new double[0];
        String s = json.trim();
        s = s.replaceAll("\\[|\\]", "");
        if (s.isBlank()) return new double[0];
        String[] parts = s.split(",\\s*");
        double[] out = new double[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Double.parseDouble(parts[i]);
        return out;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) return 0.0;
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
