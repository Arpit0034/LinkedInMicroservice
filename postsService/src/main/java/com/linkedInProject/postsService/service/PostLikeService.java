package com.linkedInProject.postsService.service;

import com.linkedInProject.postsService.entity.Post;
import com.linkedInProject.postsService.entity.PostLike;
import com.linkedInProject.postsService.exception.BadRequestException;
import com.linkedInProject.postsService.exception.ResourceNotFoundException;
import com.linkedInProject.postsService.repository.PostLikeRepository;
import com.linkedInProject.postsService.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostLikeService {

    private final PostLikeRepository postLikeRepository ;
    private final PostRepository postRepository ;
    private final ModelMapper modelMapper ;

    public void likePost(Long postId){
        Long userId = 1L ;
        log.info("User with Id: {} liking the Post with Id: {}",userId,postId);
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found with Id: "+postId)) ;
        boolean hasAlreadyLiked = postLikeRepository.existsByUserIdAndPostId(userId,postId) ;
        if(hasAlreadyLiked) throw new BadRequestException("Cannot like the post again") ;
        PostLike postLike = new PostLike() ;
        postLike.setPostId(postId);
        postLike.setUserId(userId);
        postLikeRepository.save(postLike) ;
        // TODO: send notification to owner of post
    }

    @Transactional
    public void unlikePost(Long postId) {
        Long userId = 1L ;
        log.info("User with Id: {} unliking the Post with Id: {}",userId,postId);
        postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found with Id: "+postId)) ;
        boolean hasAlreadyLiked = postLikeRepository.existsByUserIdAndPostId(userId,postId) ;
        if(!hasAlreadyLiked) throw new BadRequestException("Cannot unlike the post that you have not like") ;
        postLikeRepository.deleteByUserIdAndPostId(userId,postId) ;
        // TODO: send notification to owner of post
    }
}
