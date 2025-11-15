package com.example.demo.repository;

import com.example.demo.model.RedditPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<RedditPost, Long> {
    Optional<RedditPost> findByRedditId(String redditId);
}
