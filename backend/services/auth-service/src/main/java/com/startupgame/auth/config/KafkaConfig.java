package com.startupgame.auth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic createTopic() {
        TopicBuilder topicBuilder = TopicBuilder.name("auth-topic")
                .partitions(6)
                .replicas(3)
                .configs(Map.of("min.insync.replicas", "2"));
        return topicBuilder.build();
    }
}
