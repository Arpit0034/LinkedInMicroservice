package com.linkedInProject.notification_service.consumer;

import com.linkedInProject.ConnectionsService.event.ConnectionAccept;
import com.linkedInProject.ConnectionsService.event.ConnectionRequest;
import com.linkedInProject.notification_service.entity.Notification;
import com.linkedInProject.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionsConsumer {

    private final NotificationService notificationService ;

    @KafkaListener(topics = "connection_request_topic")
    public void handleConnectionRequestCreated(ConnectionRequest connectionRequest){
        log.info("handleConnectionRequestCreated: {}",connectionRequest);
        String message = String.format("You get connection request from user with Id: %d",connectionRequest.getSenderId());
        Notification notification = Notification.builder()
                .message(message)
                .userId(connectionRequest.getReceiverId())
                .build();
        notificationService.addNotification(notification);
    }

    @KafkaListener(topics = "connection_accept_topic")
    public void handleConnectionAcceptCreated(ConnectionAccept connectionAccept){
        log.info("handleConnectionAcceptCreated: {}",connectionAccept);
        String message = String.format("Your connection request got accepted by user with Id: %d",connectionAccept.getReceiverId());
        Notification notification = Notification.builder()
                .message(message)
                .userId(connectionAccept.getSenderId())
                .build();
        notificationService.addNotification(notification);
    }

}
