package com.linkedInProject.ConnectionsService.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectionRequest {
    private Long senderId ;
    private Long receiverId ;
}
