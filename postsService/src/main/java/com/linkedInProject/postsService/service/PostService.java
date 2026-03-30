package com.linkedInProject.postsService.service;

import com.linkedInProject.postsService.dto.PostCreateRequestDto;
import com.linkedInProject.postsService.dto.PostDto;
import com.linkedInProject.postsService.entity.Post;
import com.linkedInProject.postsService.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private PostRepository postRepository;
    private ModelMapper modelMapper;

    public PostDto createPost(PostCreateRequestDto postCreateRequestDto, Long userId){
        Post post = modelMapper.map(postCreateRequestDto,Post.class);
        post.setUserId(userId);
        post = postRepository.save(post);
        return modelMapper.map(post,PostDto.class) ;
    }
}
