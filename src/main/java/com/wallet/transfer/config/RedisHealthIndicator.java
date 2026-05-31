package com.wallet.transfer.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;

public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory connectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try{
            String pong = connectionFactory.getConnection().ping();
            if("PONG".equals(pong)){
                return Health.up().withDetail("redis", "connected").build();
            }
            return Health.down().withDetail("redis", "Unexpected Response : " + pong).build();
        } catch(Exception e){
            return Health.down().withDetail("redis", e.getMessage()).build();
        }
    }
}
