package com.wallet.transfer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topic.transfer-events}")
    private String transferEventsTopic;

    @Bean
    public NewTopic transferEventsTopic(){
        return TopicBuilder.name(transferEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

}
