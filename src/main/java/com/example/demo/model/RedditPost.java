package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reddit_posts", indexes = {
        @Index(name = "idx_reddit_id", columnList = "reddit_id")
})

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedditPost {

    // DB primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reddit's id (string) stored separately and unique
    @Column(name = "reddit_id", nullable = false, unique = true, length = 64)
    private String redditId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 200)
    private String author;

    @Column(name = "created_utc")
    private long createdUtc;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String selftext;

    @Column(length = 1000)
    private String url;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRedditId() {
        return redditId;
    }

    public void setRedditId(String redditId) {
        this.redditId = redditId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getCreatedUtc() {
        return createdUtc;
    }

    public void setCreatedUtc(long createdUtc) {
        this.createdUtc = createdUtc;
    }

    public String getSelftext() {
        return selftext;
    }

    public void setSelftext(String selftext) {
        this.selftext = selftext;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
