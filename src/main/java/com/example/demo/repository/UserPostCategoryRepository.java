package com.example.demo.repository;

import com.example.demo.model.UserPostCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPostCategoryRepository extends JpaRepository<UserPostCategory, Long> {

    // Find a user's categories for a specific post
    Optional<UserPostCategory> findByUsernameAndPostRedditId(String username, String postRedditId);

    // Delete a user's categories for a specific post
    void deleteByUsernameAndPostRedditId(String username, String postRedditId);
}
