package com.wallet.transfer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;


@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private static final String KEY_PREFIX = "idempotency";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public IdempotencyService(
            StringRedisTemplate redisTemplate,
            @Value("${app.idempotency.ttl-minutes:60}") int ttlMinutes){
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public boolean isDuplicate(String idempotencyKey){
        String key = KEY_PREFIX + idempotencyKey;
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSING", ttl);
        if(Boolean.FALSE.equals(wasAbsent)){
            log.info("Duplicate request detected for key : {}", idempotencyKey);
            return true;
        }
        return false;
    }

    public void markCompleted(String idempotencyKey, String result){
        String key = KEY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(key, result, ttl);
    }

    public void remove(String idempotencyKey){
        redisTemplate.delete(KEY_PREFIX + idempotencyKey);
    }


}
