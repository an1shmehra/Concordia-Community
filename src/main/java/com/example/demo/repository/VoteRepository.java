package com.example.demo.repository;

import com.example.demo.model.Vote;
import com.example.demo.model.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    // Find vote by username and post
    Optional<Vote> findByUsernameAndPostRedditId(String username, String postRedditId);

    // Find vote by username and comment
    Optional<Vote> findByUsernameAndCommentId(String username, Long commentId);

    // Count upvotes for a post
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.postRedditId = :postRedditId AND v.voteType = :voteType")
    long countByPostRedditIdAndVoteType(@Param("postRedditId") String postRedditId, @Param("voteType") VoteType voteType);

    // Count upvotes for a comment
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.commentId = :commentId AND v.voteType = :voteType")
    long countByCommentIdAndVoteType(@Param("commentId") Long commentId, @Param("voteType") VoteType voteType);

    // Calculate net score for a post (upvotes - downvotes)
    @Query("SELECT COALESCE(SUM(CASE WHEN v.voteType = 'UP' THEN 1 WHEN v.voteType = 'DOWN' THEN -1 ELSE 0 END), 0) " +
           "FROM Vote v WHERE v.postRedditId = :postRedditId")
    long calculatePostScore(@Param("postRedditId") String postRedditId);

    // Calculate net score for a comment (upvotes - downvotes)
    @Query("SELECT COALESCE(SUM(CASE WHEN v.voteType = 'UP' THEN 1 WHEN v.voteType = 'DOWN' THEN -1 ELSE 0 END), 0) " +
           "FROM Vote v WHERE v.commentId = :commentId")
    long calculateCommentScore(@Param("commentId") Long commentId);

    // BATCH: Get all vote scores for multiple posts at once (HUGE performance boost!)
    @Query("SELECT v.postRedditId, SUM(CASE WHEN v.voteType = 'UP' THEN 1 WHEN v.voteType = 'DOWN' THEN -1 ELSE 0 END) " +
           "FROM Vote v WHERE v.postRedditId IN :postIds GROUP BY v.postRedditId")
    java.util.List<Object[]> calculatePostScoresBatch(@Param("postIds") java.util.List<String> postIds);
}
