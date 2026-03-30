package com.linkedInProject.postsService.controller;

import com.linkedInProject.postsService.service.PostLikeService;
import com.linkedInProject.postsService.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/likes")
public class PostLikesController {

    private final PostLikeService postLikeService ;

    @PostMapping("/{postId}")
    public ResponseEntity<Void> likePost(@PathVariable Long postId){
        postLikeService.likePost(postId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> unlikePost(@PathVariable Long postId){
        postLikeService.unlikePost(postId);
        return ResponseEntity.noContent().build();
    }
}
