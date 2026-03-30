package com.linkedInProject.postsService.repository;

import com.linkedInProject.postsService.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post,Long> {
}
