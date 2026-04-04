package com.linkedInProject.postsService.event;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PostLiked {
    private Long ownerUserId ;
    private Long postId ;
    private Long likedByUserId ;
}
