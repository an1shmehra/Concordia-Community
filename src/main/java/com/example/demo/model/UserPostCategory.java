package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_post_categories")
public class UserPostCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "post_reddit_id", nullable = false)
    private String postRedditId;

    @Column(length = 500)
    private String categories;

    // Constructors
    public UserPostCategory() {}

    public UserPostCategory(String username, String postRedditId, String categories) {
        this.username = username;
        this.postRedditId = postRedditId;
        this.categories = categories;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPostRedditId() {
        return postRedditId;
    }

    public void setPostRedditId(String postRedditId) {
        this.postRedditId = postRedditId;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }
}
