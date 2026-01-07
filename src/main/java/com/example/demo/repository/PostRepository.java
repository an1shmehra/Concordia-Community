package com.example.demo.repository;

import com.example.demo.model.RedditPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface
        PostRepository extends JpaRepository<RedditPost, Long> {
    Optional<RedditPost> findByRedditId(String redditId);

    // Fetch top N posts ordered by createdUtc DESC (newest first) using database pagination
    @Query("SELECT p FROM RedditPost p ORDER BY p.createdUtc DESC")
    List<RedditPost> findTopPostsByCreatedUtc(Pageable pageable);
}
