package com.example.demo.controller;

import com.example.demo.model.RedditPost;
import com.example.demo.repository.PostRepository;
import com.example.demo.service.RedditService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Scanner;

@Controller
@RequestMapping("/api/reddit")
public class RedditController {

    private final RedditService redditService;
    private final PostRepository redditPostRepository;

    public RedditController(RedditService redditService, PostRepository redditPostRepository) {
        this.redditService = redditService;
        this.redditPostRepository = redditPostRepository;
    }


    @GetMapping("/concordia")
    public String getConcordiaPosts(Model model) {
        List<RedditPost> posts = redditService.getConcordiaPosts(20);
        model.addAttribute("posts", posts);
        return "reddit"; // Thymeleaf template
    }

    @GetMapping("/stats")
    public ResponseEntity<String> getPostStats() {

        List<RedditPost> allPosts = redditPostRepository.findAll();

        if (allPosts.isEmpty()) {
            return ResponseEntity.ok("No posts in database");
        }

        // Find oldest and newest posts
        long oldestTimestamp = allPosts.stream()
                .mapToLong(RedditPost::getCreatedUtc)
                .min()
                .orElse(0);

        long newestTimestamp = allPosts.stream()
                .mapToLong(RedditPost::getCreatedUtc)
                .max()
                .orElse(0);

        // Convert to readable dates
        java.time.Instant oldestDate = java.time.Instant.ofEpochSecond(oldestTimestamp);
        java.time.Instant newestDate = java.time.Instant.ofEpochSecond(newestTimestamp);

        long oneYearAgo = System.currentTimeMillis() / 1000 - (365L * 24 * 60 * 60);

        String result = String.format(
                "Total posts: %d\nOldest post: %s\nNewest post: %s\nOne year ago: %s\nWithin last year: %b",
                allPosts.size(),
                oldestDate,
                newestDate,
                java.time.Instant.ofEpochSecond(oneYearAgo),
                oldestTimestamp >= oneYearAgo
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/posts")
    public String getRedditPosts(Model model) {
        List<RedditPost> posts = redditService.getConcordiaPosts(20);
        model.addAttribute("posts", posts);
        return "reddit"; // Thymeleaf template
    }

    @GetMapping("/import")
    public ResponseEntity<String> importConcordiaPosts() {
        List<RedditPost> saved = redditService.fetchAndStoreOneYear(); // example
        return ResponseEntity.ok("Saved " + saved.size() + " posts");
    }

    @GetMapping("/{index}")
    public String postDetails(@PathVariable int index, Model model) {
        List<RedditPost> posts = redditService.getConcordiaPosts(20);
        if (index >= 0 && index < posts.size()) {
            RedditPost post = posts.get(index);
            model.addAttribute("post", post);
            return "reddit-details"; // Thymeleaf template
        } else {
            return "redirect:/api/reddit/posts"; // invalid index
        }
    }
}
