package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reddit_comment_id", unique = true)
    private String redditCommentId;

    @Column(name = "post_reddit_id")
    private String postRedditId;  // Which post this comment belongs to

    @Column(length = 200)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "created_utc")
    private long createdUtc;

    @Column(name = "score")
    private int score;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRedditCommentId() {
        return redditCommentId;
    }

    public void setRedditCommentId(String redditCommentId) {
        this.redditCommentId = redditCommentId;
    }

    public String getPostRedditId() {
        return postRedditId;
    }

    public void setPostRedditId(String postRedditId) {
        this.postRedditId = postRedditId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getCreatedUtc() {
        return createdUtc;
    }

    public void setCreatedUtc(long createdUtc) {
        this.createdUtc = createdUtc;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
