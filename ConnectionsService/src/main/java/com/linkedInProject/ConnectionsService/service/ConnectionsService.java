package com.linkedInProject.ConnectionsService.service;

import com.linkedInProject.ConnectionsService.auth.AuthContextHolder;
import com.linkedInProject.ConnectionsService.entity.Person;
import com.linkedInProject.ConnectionsService.event.ConnectionAccept;
import com.linkedInProject.ConnectionsService.event.ConnectionRequest;
import com.linkedInProject.ConnectionsService.exception.BadRequestException;
import com.linkedInProject.ConnectionsService.exception.ResourceNotFoundException;
import com.linkedInProject.ConnectionsService.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionsService {

    private final PersonRepository personRepository;
    private final KafkaTemplate<Long, ConnectionAccept> connectionAcceptKafkaTemplate ;
    private final KafkaTemplate<Long, ConnectionRequest> connectionRequestKafkaTemplate ;

    public List<Person> getFirstDegreeConnectionsOfUser(Long userId) {
        log.info("Getting first degree connections of user with ID: {}", userId);
        return personRepository.getFirstDegreeConnections(userId);
    }

    public void sendConnectionRequest(Long receiverId) {
        boolean receiverExists = personRepository.userExists(receiverId);
        if (!receiverExists) {
            throw new ResourceNotFoundException("Receiver not exists");
        }

        Long senderId = AuthContextHolder.getCurrentUserId();
        log.info("Sending connection request with senderId: {}, receiverId: {}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Both sender and receiver are the same");
        }

        boolean alreadySentRequest = Boolean.TRUE.equals(
                personRepository.connectionRequestExists(senderId, receiverId)
        );
        if (alreadySentRequest) {
            throw new BadRequestException("Connection request already exists, cannot send again");
        }

        boolean alreadyConnected = personRepository.alreadyConnected(senderId, receiverId);
        if (alreadyConnected) {
            throw new BadRequestException("Already connected users, cannot add connection request");
        }

        personRepository.addConnectionRequest(senderId, receiverId);
        ConnectionRequest connectionAccept = ConnectionRequest.builder()
                        .receiverId(receiverId)
                        .senderId(senderId).build();
        connectionRequestKafkaTemplate.send("connection_request_topic",connectionAccept);
        log.info("Successfully sent the connection request");
    }

    public void acceptConnectionRequest(Long senderId) {
        boolean senderExists = personRepository.userExists(senderId);
        if (!senderExists) {
            throw new ResourceNotFoundException("Sender not exists");
        }

        Long receiverId = AuthContextHolder.getCurrentUserId();
        log.info("Accepting a connection request with senderId: {}, receiverId: {}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Both sender and receiver are the same");
        }

        boolean alreadyConnected = personRepository.alreadyConnected(senderId, receiverId);
        if (alreadyConnected) {
            throw new BadRequestException("Already connected users, cannot accept connection request");
        }

        boolean alreadySentRequest = Boolean.TRUE.equals(
                personRepository.connectionRequestExists(senderId, receiverId)
        );
        if (!alreadySentRequest) {
            throw new BadRequestException("No Connection request exists, cannot accept without request");
        }

        personRepository.acceptConnectionRequest(senderId, receiverId);
        ConnectionAccept connectionAccept = ConnectionAccept.builder()
                        .senderId(senderId)
                        .receiverId(receiverId).build();
        connectionAcceptKafkaTemplate.send("connection_accept_topic",connectionAccept);
        log.info("Successfully accepted the connection request");
    }

    public void rejectConnectionRequest(Long senderId) {
        boolean senderExists = personRepository.userExists(senderId);
        if (!senderExists) {
            throw new ResourceNotFoundException("Sender not exists");
        }

        Long receiverId = AuthContextHolder.getCurrentUserId();
        log.info("Rejecting a connection request with senderId: {}, receiverId: {}", senderId, receiverId);

        boolean alreadySentRequest = Boolean.TRUE.equals(
                personRepository.connectionRequestExists(senderId, receiverId)
        );
        if (!alreadySentRequest) {
            throw new BadRequestException("No Connection request exists, cannot reject it");
        }

        personRepository.rejectConnectionRequest(senderId, receiverId);
        log.info("Successfully rejected the connection request");
    }
}