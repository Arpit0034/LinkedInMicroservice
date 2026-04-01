package com.linkedInProject.postsService.service;

import com.linkedInProject.postsService.auth.AuthContextHolder;
import com.linkedInProject.postsService.client.ConnectionsServiceClient;
import com.linkedInProject.postsService.dto.PersonDto;
import com.linkedInProject.postsService.dto.PostCreateRequestDto;
import com.linkedInProject.postsService.dto.PostDto;
import com.linkedInProject.postsService.entity.Post;
import com.linkedInProject.postsService.exception.ResourceNotFoundException;
import com.linkedInProject.postsService.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final ModelMapper modelMapper;
    private final ConnectionsServiceClient connectionsServiceClient;

    public PostDto createPost(PostCreateRequestDto postCreateRequestDto, Long userId){
        log.info("Creating post for user with Id: {}",userId) ;
        Post post = modelMapper.map(postCreateRequestDto,Post.class);
        post.setUserId(userId);
        post = postRepository.save(post);
        return modelMapper.map(post,PostDto.class) ;
    }

    public PostDto getPostById(Long postId) {
        log.info("Getting the post with Id: {}",postId) ;
        Long userId = AuthContextHolder.getCurrentUserId() ;
        // TODO: Remove in future
        // call the connection service from the Posts Service and pass the userId inside the headers

        List<PersonDto> personDtoList = connectionsServiceClient.getFirstDegreeConnections(userId);

        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found with Id: "+postId)) ;
        return modelMapper.map(post,PostDto.class) ;
    }

    public List<PostDto> getAllPostsOfUser(Long userId) {
        log.info("Getting all the posts of a user with Id: {}",userId);
        List<Post> postList = postRepository.findByUserId(userId) ;
        return postList.stream().map((element) -> modelMapper.map(element,PostDto.class)).toList() ;
    }
}
