package com.example.demo.repository;

import com.example.demo.model.UserPostCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPostCategoryRepository extends JpaRepository<UserPostCategory, Long> {

    // Find a user's categories for a specific post
    Optional<UserPostCategory> findByUsernameAndPostRedditId(String username, String postRedditId);

    // Delete a user's categories for a specific post
    void deleteByUsernameAndPostRedditId(String username, String postRedditId);

    // BATCH: Get all user categories for multiple posts at once (HUGE performance boost!)
    @Query("SELECT upc FROM UserPostCategory upc WHERE upc.username = :username AND upc.postRedditId IN :postIds")
    List<UserPostCategory> findByUsernameAndPostRedditIdIn(@Param("username") String username, @Param("postIds") List<String> postIds);
}
