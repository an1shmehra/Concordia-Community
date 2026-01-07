package com.example.demo.controller;

import com.example.demo.model.Comment;
import com.example.demo.model.RedditPost;
import com.example.demo.model.UserPostCategory;
import com.example.demo.model.Vote;
import com.example.demo.model.VoteType;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserPostCategoryRepository;
import com.example.demo.repository.VoteRepository;
import com.example.demo.service.RedditService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Scanner;

@Controller
@RequestMapping("/api/reddit")
public class RedditController {

    private final RedditService redditService;
    private final PostRepository redditPostRepository;
    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository;
    private final com.example.demo.service.AICategorizationService aiCategorizationService;
    private final com.example.demo.repository.UserPostCategoryRepository userPostCategoryRepository;

    public RedditController(RedditService redditService, PostRepository redditPostRepository,
                          CommentRepository commentRepository, VoteRepository voteRepository,
                          com.example.demo.service.AICategorizationService aiCategorizationService,
                          com.example.demo.repository.UserPostCategoryRepository userPostCategoryRepository) {
        this.redditService = redditService;
        this.redditPostRepository = redditPostRepository;
        this.commentRepository = commentRepository;
        this.voteRepository = voteRepository;
        this.aiCategorizationService = aiCategorizationService;
        this.userPostCategoryRepository = userPostCategoryRepository;
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

/*    @GetMapping("/posts")
    public String getRedditPosts(Model model) {
        List<RedditPost> posts = redditService.getConcordiaPosts(20);
        model.addAttribute("posts", posts);
        return "reddit"; // Thymeleaf template
    }*/

    @GetMapping("/import")
    public ResponseEntity<String> importConcordiaPosts() {
        List<RedditPost> saved = redditService.fetchAndStoreOneYear(); // example
        return ResponseEntity.ok("Saved " + saved.size() + " posts");
    }

    @GetMapping("/posts")
    public String getRedditPosts(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "sort", required = false, defaultValue = "new") String sort,
            java.security.Principal principal,
            Model model) {
        // GET FROM DATABASE - Use pagination to fetch only top 250 posts
        List<RedditPost> posts = redditPostRepository.findTopPostsByCreatedUtc(
                PageRequest.of(0, 250)
        );

        String trimmed = (query != null) ? query.trim() : null;
        if (trimmed != null && !trimmed.isEmpty()) {
            String lowerQuery = trimmed.toLowerCase();

            posts = posts.stream()
                    .filter(post ->
                            (post.getTitle() != null && post.getTitle().toLowerCase().contains(lowerQuery)) ||
                                    (post.getSelftext() != null && post.getSelftext().toLowerCase().contains(lowerQuery))
                    )
                    .collect(Collectors.toList());

            if (posts.isEmpty()) {
                model.addAttribute("notFoundMessage", "Not found");
            }
        }

        // BATCH QUERY: Get ALL vote scores in ONE query instead of 250 individual queries!
        List<String> postIds = posts.stream().map(RedditPost::getRedditId).collect(Collectors.toList());
        Map<String, Long> voteScores = new HashMap<>();

        if (!postIds.isEmpty()) {
            List<Object[]> scoresFromDB = voteRepository.calculatePostScoresBatch(postIds);
            for (Object[] row : scoresFromDB) {
                String postId = (String) row[0];
                Long score = ((Number) row[1]).longValue();
                voteScores.put(postId, score);
            }
        }

        // Set default score of 0 for posts with no votes
        for (RedditPost post : posts) {
            voteScores.putIfAbsent(post.getRedditId(), 0L);
        }

        // Apply sorting
        switch (sort.toLowerCase()) {
            case "top":
                // Sort by vote score (highest first)
                posts.sort((a, b) -> Long.compare(
                        voteScores.getOrDefault(b.getRedditId(), 0L),
                        voteScores.getOrDefault(a.getRedditId(), 0L)
                ));
                break;
            case "hot":
                // Sort by "hot" algorithm (score / age factor)
                long currentTime = System.currentTimeMillis() / 1000;
                posts.sort((a, b) -> {
                    double hotScoreA = calculateHotScore(
                            voteScores.getOrDefault(a.getRedditId(), 0L),
                            a.getCreatedUtc(),
                            currentTime
                    );
                    double hotScoreB = calculateHotScore(
                            voteScores.getOrDefault(b.getRedditId(), 0L),
                            b.getCreatedUtc(),
                            currentTime
                    );
                    return Double.compare(hotScoreB, hotScoreA);
                });
                break;
            case "new":
            default:
                // Sort by newest first (default)
                posts.sort((a, b) -> Long.compare(b.getCreatedUtc(), a.getCreatedUtc()));
                break;
        }

        // BATCH QUERY: Fetch user-specific categories for all posts in ONE query!
        Map<String, String> userCategories = new HashMap<>();
        if (principal != null) {
            String username = principal.getName();
            List<UserPostCategory> userCategoriesList = userPostCategoryRepository.findByUsernameAndPostRedditIdIn(username, postIds);
            for (UserPostCategory upc : userCategoriesList) {
                userCategories.put(upc.getPostRedditId(), upc.getCategories());
            }
        }

        model.addAttribute("posts", posts);
        model.addAttribute("voteScores", voteScores);
        model.addAttribute("userCategories", userCategories);
        model.addAttribute("query", (trimmed != null && !trimmed.isEmpty()) ? trimmed : null);
        model.addAttribute("sort", sort);
        return "reddit";
    }

    // Hot algorithm: score / ((age_in_hours + 2) ^ 1.5)
    private double calculateHotScore(long score, long postTime, long currentTime) {
        long ageInSeconds = currentTime - postTime;
        double ageInHours = ageInSeconds / 3600.0;
        // Add 2 hours to prevent division by zero and reduce impact of very new posts
        double ageFactor = Math.pow(ageInHours + 2, 1.5);
        // Add 1 to score to handle posts with 0 or negative scores
        return (score + 1) / ageFactor;
    }


    @GetMapping("/{index}")
    public String postDetails(@PathVariable int index, Model model) {
        // Get from database using pagination (matches main page)
        List<RedditPost> posts = redditPostRepository.findTopPostsByCreatedUtc(
                PageRequest.of(0, 250)
        );

        if (index >= 0 && index < posts.size()) {
            RedditPost post = posts.get(index);

            // Get comments for this post
            List<Comment> comments = commentRepository.findByPostRedditId(post.getRedditId());

            // Calculate vote scores for each comment
            Map<Long, Long> commentVoteScores = new HashMap<>();
            for (Comment comment : comments) {
                long score = voteRepository.calculateCommentScore(comment.getId());
                commentVoteScores.put(comment.getId(), score);
            }

            // Calculate vote score for the post
            long postScore = voteRepository.calculatePostScore(post.getRedditId());

            model.addAttribute("post", post);
            model.addAttribute("postScore", postScore);
            model.addAttribute("comments", comments);
            model.addAttribute("commentVoteScores", commentVoteScores);
            return "reddit-details";
        } else {
            return "redirect:/api/reddit/posts";
        }
    }

    // Import comments for ALL posts
    @GetMapping("/import-comments")
    public ResponseEntity<String> importComments() {
        List<RedditPost> posts = redditPostRepository.findAll();
        posts.sort((a, b) -> Long.compare(b.getCreatedUtc(), a.getCreatedUtc()));

        int totalSaved = 0;
        int skipped = 0;
        int processed = 0;

        for (RedditPost post : posts) {
            processed++;
            System.out.println("Processing post " + processed + "/" + posts.size() + ": " + post.getTitle());

            List<com.example.demo.model.Comment> comments = redditService.fetchCommentsForPost(post.getRedditId());
            for (com.example.demo.model.Comment comment : comments) {
                if (comment.getRedditCommentId() != null && !comment.getRedditCommentId().isEmpty()) {
                    // Check if comment already exists
                    boolean exists = commentRepository.findByRedditCommentId(comment.getRedditCommentId()).isPresent();
                    if (!exists) {
                        commentRepository.save(comment);
                        totalSaved++;
                    } else {
                        skipped++;
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return ResponseEntity.ok("Imported " + totalSaved + " new comments (skipped " + skipped + " duplicates) for " + posts.size() + " posts!");
    }

    // Show create post form
    @GetMapping("/create")
    public String showCreateForm() {
        return "create-post";
    }

    // Handle post creation
    @org.springframework.web.bind.annotation.PostMapping("/create")
    public String createPost(
            @RequestParam String title,
            @RequestParam(required = false) String selftext,
            @RequestParam(required = false) String url,
            java.security.Principal principal) {

        // Generate a unique reddit_id
        String redditId = "user_" + System.currentTimeMillis();

        // Get username from authenticated user
        String author = (principal != null) ? principal.getName() : "Anonymous";

        // Create new post
        RedditPost newPost = new RedditPost();
        newPost.setRedditId(redditId);
        newPost.setTitle(title);
        newPost.setSelftext(selftext);
        newPost.setUrl(url);
        newPost.setAuthor(author);
        newPost.setCreatedUtc(System.currentTimeMillis() / 1000); // Current time in seconds

        // Save to database
        redditPostRepository.save(newPost);

        // Redirect back to posts list
        return "redirect:/api/reddit/posts";
    }

    // Handle comment creation
    @org.springframework.web.bind.annotation.PostMapping("/comment/add")
    public String addComment(
            @RequestParam String postRedditId,
            @RequestParam String body,
            java.security.Principal principal) {

        // Generate a unique comment ID
        String commentId = "user_comment_" + System.currentTimeMillis();

        // Get username from authenticated user
        String author = (principal != null) ? principal.getName() : "Anonymous";

        // Create new comment
        Comment newComment = new Comment();
        newComment.setRedditCommentId(commentId);
        newComment.setPostRedditId(postRedditId);
        newComment.setAuthor(author);
        newComment.setBody(body);
        newComment.setCreatedUtc(System.currentTimeMillis() / 1000); // Current time in seconds
        newComment.setScore(1); // Default score

        // Save to database
        commentRepository.save(newComment);

        // Find the post index to redirect back to the same post
        List<RedditPost> posts = redditPostRepository.findAll();
        posts.sort((a, b) -> Long.compare(b.getCreatedUtc(), a.getCreatedUtc()));

        int index = 0;
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getRedditId().equals(postRedditId)) {
                index = i;
                break;
            }
        }

        // Redirect back to post details
        return "redirect:/api/reddit/" + index;
    }

    // Vote on a post
    @org.springframework.web.bind.annotation.PostMapping("/vote/post/{redditId}")
    public String voteOnPost(
            @PathVariable String redditId,
            @RequestParam String voteType,
            java.security.Principal principal,
            jakarta.servlet.http.HttpSession session) {

        // Use username if logged in, otherwise use session ID
        String username = (principal != null) ? principal.getName() : "anon_" + session.getId();
        VoteType type = "up".equals(voteType) ? VoteType.UP : VoteType.DOWN;

        // Check if user already voted on this post
        var existingVote = voteRepository.findByUsernameAndPostRedditId(username, redditId);

        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();
            if (vote.getVoteType() == type) {
                // Same vote - remove it (toggle off)
                voteRepository.delete(vote);
            } else {
                // Different vote - update it
                vote.setVoteType(type);
                voteRepository.save(vote);
            }
        } else {
            // Create new vote
            Vote newVote = new Vote();
            newVote.setUsername(username);
            newVote.setPostRedditId(redditId);
            newVote.setVoteType(type);
            voteRepository.save(newVote);
        }

        return "redirect:/api/reddit/posts";
    }

    // Vote on a comment
    @org.springframework.web.bind.annotation.PostMapping("/vote/comment/{commentId}")
    public String voteOnComment(
            @PathVariable Long commentId,
            @RequestParam String voteType,
            @RequestParam String postRedditId,
            java.security.Principal principal,
            jakarta.servlet.http.HttpSession session) {

        // Use username if logged in, otherwise use session ID
        String username = (principal != null) ? principal.getName() : "anon_" + session.getId();
        VoteType type = "up".equals(voteType) ? VoteType.UP : VoteType.DOWN;

        // Check if user already voted on this comment
        var existingVote = voteRepository.findByUsernameAndCommentId(username, commentId);

        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();
            if (vote.getVoteType() == type) {
                // Same vote - remove it (toggle off)
                voteRepository.delete(vote);
            } else {
                // Different vote - update it
                vote.setVoteType(type);
                voteRepository.save(vote);
            }
        } else {
            // Create new vote
            Vote newVote = new Vote();
            newVote.setUsername(username);
            newVote.setCommentId(commentId);
            newVote.setVoteType(type);
            voteRepository.save(newVote);
        }

        // Find post index and redirect back to post details
        List<RedditPost> posts = redditPostRepository.findAll();
        posts.sort((a, b) -> Long.compare(b.getCreatedUtc(), a.getCreatedUtc()));

        int index = 0;
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getRedditId().equals(postRedditId)) {
                index = i;
                break;
            }
        }

        return "redirect:/api/reddit/" + index;
    }

    // AI Categorization endpoint
    @org.springframework.web.bind.annotation.PostMapping("/categorize/{redditId}")
    public String categorizePost(@PathVariable String redditId, java.security.Principal principal) {
        // Only logged-in users can categorize
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        System.out.println("========================================");
        System.out.println("CATEGORIZATION REQUEST for redditId: " + redditId + " by user: " + username);
        System.out.println("========================================");

        RedditPost post = redditPostRepository.findAll().stream()
                .filter(p -> p.getRedditId().equals(redditId))
                .findFirst()
                .orElse(null);

        if (post != null) {
            System.out.println("Post found: " + post.getTitle());
            String categories = aiCategorizationService.categorizePost(post.getTitle(), post.getSelftext());
            System.out.println("Categories returned: " + categories);

            // Save user-specific categories
            var existingCategory = userPostCategoryRepository.findByUsernameAndPostRedditId(username, redditId);
            if (existingCategory.isPresent()) {
                // Update existing
                com.example.demo.model.UserPostCategory upc = existingCategory.get();
                upc.setCategories(categories);
                userPostCategoryRepository.save(upc);
            } else {
                // Create new
                com.example.demo.model.UserPostCategory upc = new com.example.demo.model.UserPostCategory(
                    username, redditId, categories
                );
                userPostCategoryRepository.save(upc);
            }
            System.out.println("User categories saved");
        } else {
            System.out.println("ERROR: Post not found with redditId: " + redditId);
        }

        return "redirect:/api/reddit/posts";
    }
}
