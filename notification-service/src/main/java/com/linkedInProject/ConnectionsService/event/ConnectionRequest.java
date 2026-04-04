package com.linkedInProject.ConnectionsService.event;

import lombok.Data;

@Data
public class ConnectionRequest {
    private Long senderId ;
    private Long receiverId ;
}
