package com.example.demo.service;

import com.example.demo.model.Comment;
import com.example.demo.model.RedditPost;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class RedditService {

    private final RestTemplate restTemplate;
    private final RedditAuthService authService;
    private final ObjectMapper objectMapper;
    private final String userAgent;
    private final PostRepository redditPostRepository;
    private final CommentRepository commentRepository;

    public RedditService(RestTemplate restTemplate,
                         RedditAuthService authService,
                         @Value("${reddit.user.agent}") String userAgent,
                         PostRepository redditPostRepository,
                         CommentRepository commentRepository) {
        this.restTemplate = restTemplate;
        this.authService = authService;
        this.userAgent = userAgent;
        this.redditPostRepository = redditPostRepository;
        this.commentRepository = commentRepository;
        this.objectMapper = new ObjectMapper();
    }

    public List<RedditPost> getConcordiaPosts(int limit) {
        List<RedditPost> posts = new ArrayList<>();
        String url = "https://oauth.reddit.com/r/concordia/new?limit=" + limit;


        try {
            String token = authService.getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("User-Agent", userAgent);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            posts = parsePosts(response.getBody());

        } catch (HttpClientErrorException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return posts;
    }
/*

    @Transactional
    public List<RedditPost> fetchAndStore(int limit) {
        List<RedditPost> saved = new ArrayList<>();
        String after = null;
        int fetched = 0;

        try {
            String token = authService.getAccessToken();

            // Keep fetching until we reach the limit or run out of posts
            while (fetched < limit) {
                int batchSize = Math.min(100, limit - fetched); // Reddit max is 100 per request
                String url = "https://oauth.reddit.com/r/concordia/new?limit=" + batchSize;

                // Add 'after' parameter for pagination
                if (after != null) {
                    url += "&after=" + after;
                }

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + token);
                headers.set("User-Agent", userAgent);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                // Parse the response
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");

                // Get the 'after' token for next page
                after = data.path("after").asText(null);

                // Parse posts
                List<RedditPost> scraped = parsePostsFromChildren(data.path("children"));
                if (scraped.isEmpty()) break; // No more posts

                // Filter and save
                List<RedditPost> toSave = new ArrayList<>();
                for (RedditPost p : scraped) {
                    if (p.getRedditId() == null || p.getRedditId().isBlank()) continue;
                    boolean exists = redditPostRepository.findByRedditId(p.getRedditId()).isPresent();
                    if (!exists) {
                        toSave.add(p);
                    }
                }

                if (!toSave.isEmpty()) {
                    saved.addAll(redditPostRepository.saveAll(toSave));
                }

                fetched += scraped.size();

                // If there's no 'after' token, we've reached the end
                if (after == null || after.isBlank()) break;

                // Be nice to Reddit's API - add a small delay
                Thread.sleep(1000); // 1 second delay between requests
            }

        } catch (HttpClientErrorException e) {
            System.out.println("Reddit API HTTP error: " + e.getStatusCode());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Error fetching/storing Reddit posts: " + e.getMessage());
            e.printStackTrace();
        }

        return saved;
    }
*/

    @Transactional
    public List<RedditPost> fetchAndStoreOneYear() {
        List<RedditPost> saved = new ArrayList<>();
        String after = null;
        int fetched = 0;

        // Calculate 1 year ago in epoch seconds
        long oneYearAgo = System.currentTimeMillis() / 1000 - (365L * 24 * 60 * 60);

        try {
            String token = authService.getAccessToken();

            while (true) { // Keep going until we hit the time limit
                String url = "https://oauth.reddit.com/r/concordia/new?limit=100";

                if (after != null) {
                    url += "&after=" + after;
                }

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + token);
                headers.set("User-Agent", userAgent);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");

                after = data.path("after").asText(null);
                List<RedditPost> scraped = parsePostsFromChildren(data.path("children"));

                System.out.println("=== Batch Info ===");
                System.out.println("Posts in this batch: " + scraped.size());
                System.out.println("Total fetched so far: " + (fetched + scraped.size()));

                if (scraped.isEmpty()) {
                    System.out.println("No more posts available");
                    break;
                }

                // Check if we've gone past 1 year
                long oldestPostTime = scraped.get(scraped.size() - 1).getCreatedUtc();
                if (oldestPostTime < oneYearAgo) {
                    System.out.println("Reached posts older than 1 year!");
                    // Filter out posts older than 1 year from this batch
                    scraped = scraped.stream()
                            .filter(p -> p.getCreatedUtc() >= oneYearAgo)
                            .collect(java.util.stream.Collectors.toList());
                }

                // Filter and save
                List<RedditPost> toSave = new ArrayList<>();
                for (RedditPost p : scraped) {
                    if (p.getRedditId() == null || p.getRedditId().isBlank()) continue;
                    boolean exists = redditPostRepository.findByRedditId(p.getRedditId()).isPresent();
                    if (!exists) {
                        toSave.add(p);
                    }
                }

                if (!toSave.isEmpty()) {
                    saved.addAll(redditPostRepository.saveAll(toSave));
                }

                fetched += scraped.size();

                // Stop if we've gone past 1 year
                if (oldestPostTime < oneYearAgo) {
                    break;
                }

                // Stop if no more posts available
                if (after == null || after.isBlank()) {
                    System.out.println("No more posts available from Reddit API!");
                    break;
                }

                Thread.sleep(1000); // Rate limiting
            }

            System.out.println("FINAL: Saved " + saved.size() + " new posts from last year, fetched " + fetched + " total");

        } catch (HttpClientErrorException e) {
            System.out.println("Reddit API HTTP error: " + e.getStatusCode());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Error fetching/storing Reddit posts: " + e.getMessage());
            e.printStackTrace();
        }

        return saved;
    }
    private List<RedditPost> parsePostsFromChildren(JsonNode children) {
        List<RedditPost> posts = new ArrayList<>();

        for (JsonNode child : children) {
            JsonNode d = child.path("data");

            String redditId = d.path("id").asText(null);
            if (redditId == null || redditId.isBlank()) {
                redditId = d.path("name").asText(null);
            }

            String title = d.path("title").asText("");
            String author = d.path("author").asText("[deleted]");
            String url = d.path("url").asText("");
            String permalink = d.path("permalink").asText(null);
            String redditLink = (permalink != null && !permalink.isBlank())
                    ? "https://reddit.com" + permalink
                    : url;

            String selftext = d.path("selftext").asText("");
            long createdUtc = d.path("created_utc").asLong(0L);
            int numComments = d.path("num_comments").asInt(0);

            RedditPost post = new RedditPost();
            post.setRedditId(redditId);
            post.setTitle(title);
            post.setAuthor(author);
            post.setUrl(url);
            post.setSelftext(selftext);
            post.setCreatedUtc(createdUtc);

            posts.add(post);
        }
        return posts;
    }


    private List<RedditPost> parsePosts(String json) throws Exception {
        List<RedditPost> posts = new ArrayList<>();
        JsonNode children = objectMapper.readTree(json).path("data").path("children");

        for (JsonNode child : children) {
            JsonNode d = child.path("data");

            // reddit id: prefer 'id', fall back to 'name' (which is like "t3_xxx")
            String redditId = d.path("id").asText(null);
            if (redditId == null || redditId.isBlank()) {
                redditId = d.path("name").asText(null);
            }

            String title = d.path("title").asText("");
            String author = d.path("author").asText("[deleted]");
            String url = d.path("url").asText("");
            String permalink = d.path("permalink").asText(null);
            // optionally prefer permalink for linking back to reddit:
            String redditLink = (permalink != null && !permalink.isBlank())
                    ? "https://reddit.com" + permalink
                    : url;

            String selftext = d.path("selftext").asText("");
            long createdUtc = d.path("created_utc").asLong(0L); // epoch seconds
            int numComments = d.path("num_comments").asInt(0);

            RedditPost post = new RedditPost();
            post.setRedditId(redditId);
            post.setTitle(title);
            post.setAuthor(author);
            post.setUrl(url);
            post.setSelftext(selftext);
            post.setCreatedUtc(createdUtc);

            // if your entity has a field for numComments, set it too

            posts.add(post);
        }
        return posts;
    }

    // Fetch top 3 comments for a specific post
    public List<Comment> fetchCommentsForPost(String postId) {
        List<Comment> comments = new ArrayList<>();

        try {
            String token = authService.getAccessToken();
            String url = "https://oauth.reddit.com/r/concordia/comments/" + postId + "?limit=3";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("User-Agent", userAgent);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // Reddit returns an array: [0] = post, [1] = comments
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.isArray() && root.size() > 1) {
                JsonNode commentsListing = root.get(1);
                JsonNode commentsData = commentsListing.path("data").path("children");

                int count = 0;
                for (JsonNode commentNode : commentsData) {
                    if (count >= 3) break;  // Only top 3

                    JsonNode data = commentNode.path("data");
                    String kind = commentNode.path("kind").asText("");

                    // Skip "more" type nodes
                    if (!"t1".equals(kind)) continue;

                    Comment comment = new Comment();
                    comment.setRedditCommentId(data.path("id").asText(""));
                    comment.setPostRedditId(postId);
                    comment.setAuthor(data.path("author").asText("[deleted]"));
                    comment.setBody(data.path("body").asText(""));
                    comment.setCreatedUtc(data.path("created_utc").asLong(0L));
                    comment.setScore(data.path("score").asInt(0));

                    comments.add(comment);
                    count++;
                }
            }
        } catch (Exception e) {
            System.out.println("Error fetching comments for post " + postId + ": " + e.getMessage());
        }

        return comments;
    }

    // Import comments for all existing posts (top 3 per post)
    @Transactional
    public int importCommentsForAllPosts() {
        List<RedditPost> posts = redditPostRepository.findAll();
        int totalSaved = 0;

        for (RedditPost post : posts) {
            List<Comment> comments = fetchCommentsForPost(post.getRedditId());

            for (Comment comment : comments) {
                // Check if comment already exists
                if (comment.getRedditCommentId() != null && !comment.getRedditCommentId().isEmpty()) {
                    commentRepository.save(comment);
                    totalSaved++;
                }
            }

            // Be nice to Reddit API
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return totalSaved;
    }

}
