package com.ai.library.controller;

import com.ai.library.model.Discussion;
import com.ai.library.repository.DiscussionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {
    private final DiscussionRepository discussionRepository;

    public DiscussionController(DiscussionRepository discussionRepository) {
        this.discussionRepository = discussionRepository;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Discussion d) {
        return ResponseEntity.ok(discussionRepository.save(d));
    }

    @GetMapping
    public ResponseEntity<List<Discussion>> list() {
        return ResponseEntity.ok(discussionRepository.findAll());
    }
}
