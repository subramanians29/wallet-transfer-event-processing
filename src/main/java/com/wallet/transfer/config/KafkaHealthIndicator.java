package com.wallet.transfer.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.KafkaHeaders;

import java.util.concurrent.TimeUnit;

public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin){
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health(){
        try(AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())){
            DescribeClusterResult cluster = client.describeCluster();
            String clusterId = cluster.clusterId().get(5, TimeUnit.SECONDS);
            int nodeCount = cluster.nodes().get(5, TimeUnit.SECONDS).size();
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodeCount)
                    .build();
        } catch(Exception e){
            return Health.down().withDetail("kafka", e.getMessage()).build();
        }
    }

}
