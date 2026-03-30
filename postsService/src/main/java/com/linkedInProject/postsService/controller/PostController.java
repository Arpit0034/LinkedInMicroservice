package com.linkedInProject.postsService.controller;

import com.linkedInProject.postsService.dto.PostCreateRequestDto;
import com.linkedInProject.postsService.dto.PostDto;
import com.linkedInProject.postsService.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/core")
public class PostController {

    private final PostService postService ;

    @PostMapping
    public ResponseEntity<PostDto> createPost(@RequestBody PostCreateRequestDto postCreateRequestDto){
        PostDto postDto = postService.createPost(postCreateRequestDto,1L) ;
        return new ResponseEntity<>(postDto, HttpStatus.CREATED) ;
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDto> getPostById(@PathVariable Long postId){
        PostDto postDto = postService.getPostById(postId) ;
        return ResponseEntity.ok(postDto) ;
    }

    @GetMapping("/users/{userId}/allPosts")
    public ResponseEntity<List<PostDto>> getAllPostsOfUser(@PathVariable Long userId){
        List<PostDto> posts = postService.getAllPostsOfUser(userId) ;
        return ResponseEntity.ok(posts) ;
    }
}
