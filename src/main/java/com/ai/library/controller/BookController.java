package com.ai.library.controller;

import com.ai.library.model.Book;
import com.ai.library.service.BookService;
import com.ai.library.service.FileStorageService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.PathResource;
import java.nio.file.Path;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    private final FileStorageService fileStorageService;

    public BookController(BookService bookService, FileStorageService fileStorageService) {
        this.bookService = bookService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<?> create(@RequestBody Book book) {
        return ResponseEntity.ok(bookService.save(book));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Book>> search(@RequestParam String q) {
        return ResponseEntity.ok(bookService.search(q));
    }

    @PostMapping("/upload/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<?> uploadEbook(@PathVariable Long id, @RequestParam MultipartFile file) {
        var bookOpt = bookService.findById(id);
        if (bookOpt.isEmpty()) return ResponseEntity.notFound().build();
        String orig = file.getOriginalFilename();
        String fname = "book-" + id + "-" + (orig == null ? "ebook" : orig.replaceAll("[^a-zA-Z0-9._-]", "_"));
        String stored = fileStorageService.store(file, fname);
        Book updated = bookService.updateEbookPath(id, stored);
        return ResponseEntity.ok(Map.of("path", stored, "bookId", updated == null ? id : updated.getId()));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadEbook(@PathVariable Long id) {
    var bOpt = bookService.findById(id);
    if (bOpt.isEmpty()) return ResponseEntity.notFound().build();
    var book = bOpt.get();
    if (book.getEbookPath() == null) return ResponseEntity.notFound().build();
    Path p = Path.of(book.getEbookPath());
    Resource r = new PathResource(p);
    String fname = p.getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=\"" + fname + "\"")
        .body(r);
    }
}
