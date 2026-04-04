package com.linkedInProject.ConnectionsService.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic connectionRequest(){
        return new NewTopic("connection_request_topic",3,(short)1);
    }

    @Bean
    public NewTopic connectionAccept(){
        return new NewTopic("connection_accept_topic",3,(short)1);
    }
}
