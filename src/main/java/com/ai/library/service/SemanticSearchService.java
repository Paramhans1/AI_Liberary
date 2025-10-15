package com.ai.library.service;

import com.ai.library.ai.OpenAiClient;
import com.ai.library.model.Book;
import com.ai.library.model.DocumentChunk;
import com.ai.library.repository.BookRepository;
import com.ai.library.repository.DocumentChunkRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SemanticSearchService {
    private final OpenAiClient openAiClient;
    private final BookRepository bookRepository;
    private final DocumentChunkRepository chunkRepository;

    public SemanticSearchService(OpenAiClient openAiClient, BookRepository bookRepository, DocumentChunkRepository chunkRepository) {
        this.openAiClient = openAiClient;
        this.bookRepository = bookRepository;
        this.chunkRepository = chunkRepository;
    }

    // Naive chunking: split by paragraphs and ensure length limit
    public List<DocumentChunk> ingestBook(Long bookId) {
        Optional<Book> ob = bookRepository.findById(bookId);
        if (ob.isEmpty()) return Collections.emptyList();
        Book b = ob.get();
    String text = b.getSummary() != null ? b.getSummary() : b.getTitle();
        List<String> paragraphs = Arrays.stream(text.split("\n\n|\r\n\r\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<DocumentChunk> saved = new ArrayList<>();
        for (String p : paragraphs) {
            // further split long paragraphs
            for (int i = 0; i < p.length(); i += 1000) {
                int end = Math.min(p.length(), i + 1000);
                String chunkText = p.substring(i, end);
                DocumentChunk dc = new DocumentChunk();
                dc.setBookId(bookId);
                dc.setContent(chunkText);
                // generate embedding
                double[] emb = openAiClient.generateEmbedding(chunkText);
                dc.setEmbeddingJson(Arrays.toString(emb));
                saved.add(chunkRepository.save(dc));
            }
        }
        return saved;
    }

    // simple search: compute embedding for query, then cosine similarity against stored chunk embeddings
    public List<SearchResult> search(String query, int topN) {
        double[] qEmb = openAiClient.generateEmbedding(query);
        List<DocumentChunk> all = chunkRepository.findAll();
        PriorityQueue<SearchResult> pq = new PriorityQueue<>(Comparator.comparingDouble(r -> r.score));
        for (DocumentChunk c : all) {
            String ej = c.getEmbeddingJson();
            double[] emb = parseEmbedding(ej);
            double score = cosineSimilarity(qEmb, emb);
            if (pq.size() < topN) pq.add(new SearchResult(c, score));
            else if (pq.peek().score < score) {
                pq.poll();
                pq.add(new SearchResult(c, score));
            }
        }
        List<SearchResult> out = new ArrayList<>(pq);
        out.sort(Comparator.comparingDouble(r -> -r.score));
        return out;
    }

    private double[] parseEmbedding(String json) {
        // simple parser of format like [0.1, 0.2]
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

    public static class SearchResult {
        public final DocumentChunk chunk;
        public final double score;
        public SearchResult(DocumentChunk chunk, double score) { this.chunk = chunk; this.score = score; }
    }
}
