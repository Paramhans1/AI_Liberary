package com.ai.library.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private String isbn;
    private String genre;
    @Column(length = 4000)
    private String summary;
    private String tags; // comma separated tags for simplicity

    private Integer totalCopies = 1;
    private Integer availableCopies = 1;

    private String ebookPath; // path to uploaded PDF
    private LocalDate addedAt = LocalDate.now();
    @Lob
    private String embeddingJson; // JSON string of embedding array

    public Book() {}

    public Book(Long id, String title, String author, String isbn, String genre, String summary, String tags, Integer totalCopies, Integer availableCopies, String ebookPath, LocalDate addedAt) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.genre = genre;
        this.summary = summary;
        this.tags = tags;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.ebookPath = ebookPath;
        this.addedAt = addedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Integer getTotalCopies() { return totalCopies; }
    public void setTotalCopies(Integer totalCopies) { this.totalCopies = totalCopies; }
    public Integer getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
    public String getEbookPath() { return ebookPath; }
    public void setEbookPath(String ebookPath) { this.ebookPath = ebookPath; }
    public LocalDate getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDate addedAt) { this.addedAt = addedAt; }
    public String getEmbeddingJson() { return embeddingJson; }
    public void setEmbeddingJson(String embeddingJson) { this.embeddingJson = embeddingJson; }
}
