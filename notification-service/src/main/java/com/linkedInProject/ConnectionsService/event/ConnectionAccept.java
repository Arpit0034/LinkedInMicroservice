package com.linkedInProject.ConnectionsService.event;

import lombok.Data;

@Data
public class ConnectionAccept {
    private Long senderId ;
    private Long receiverId ;
}
