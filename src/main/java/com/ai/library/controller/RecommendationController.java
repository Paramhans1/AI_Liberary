package com.ai.library.controller;

import com.ai.library.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/books/{id}")
    public List<RecommendationService.Recommendation> recommendForBook(@PathVariable("id") Long id, @RequestParam(name = "top", defaultValue = "5") int top) {
        return recommendationService.recommendForBook(id, top);
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshEmbeddings() {
        // run async to avoid blocking caller
        new Thread(() -> {
            recommendationService.ensureEmbeddingsForAllBooks();
        }).start();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("refresh-started");
    }
}
