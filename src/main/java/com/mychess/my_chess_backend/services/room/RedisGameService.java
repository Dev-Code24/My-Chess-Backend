package com.mychess.my_chess_backend.services.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychess.my_chess_backend.dtos.redis.MoveCache;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisGameService {
  private final RedisTemplate<String, Object> redisTemplate;
  private final StringRedisTemplate stringRedisTemplate;
  private final EmergencyBufferService emergencyBuffer;
  private final ObjectMapper objectMapper;

  private static final String ROOM_KEY_PREFIX = "room_cache:";
  private static final String SYNC_SET_KEY = "rooms_to_sync";

  public RedisGameService(
      RedisTemplate<String, Object> redisTemplate,
      StringRedisTemplate stringRedisTemplate,
      EmergencyBufferService emergencyBuffer,
      ObjectMapper objectMapper
  ) {
    this.redisTemplate = redisTemplate;
    this.stringRedisTemplate = stringRedisTemplate;
    this.emergencyBuffer = emergencyBuffer;
    this.objectMapper = objectMapper;
  }

  @CircuitBreaker(name = "redisService", fallbackMethod = "bufferMoveLocally")
  public void saveMoveCache(String code, MoveCache cache) {
    String key = ROOM_KEY_PREFIX + code;
    redisTemplate.opsForValue().set(key, cache, 24, TimeUnit.HOURS);
    stringRedisTemplate.opsForSet().add(SYNC_SET_KEY, code);
  }

  public MoveCache getMoveCache(String code) {
    String key = ROOM_KEY_PREFIX + code;
    Object value = redisTemplate.opsForValue().get(key);

    if (value == null) { return null; }

    return objectMapper.convertValue(value, MoveCache.class);
  }

  public void bufferMoveLocally(String code, MoveCache cache, Throwable t) {
    emergencyBuffer.bufferMove(code, cache);
  }
}