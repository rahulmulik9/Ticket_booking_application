package com.rahul.cinemaservice.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String CINEMA_EVENTS = "cinema-events";

    @Bean
    public NewTopic cinemaEventsTopic() {
        return TopicBuilder.name(CINEMA_EVENTS).partitions(3).replicas(1).build();
    }
}