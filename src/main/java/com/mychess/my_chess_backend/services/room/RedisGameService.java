package com.mychess.my_chess_backend.services.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychess.my_chess_backend.dtos.redis.MoveCache;
import com.mychess.my_chess_backend.exceptions.room.RoomNotFoundException;
import com.mychess.my_chess_backend.models.Room;
import com.mychess.my_chess_backend.repositories.RoomRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisGameService {
  private final RedisTemplate<String, Object> redisTemplate;
  private final StringRedisTemplate stringRedisTemplate;
  private final RoomRepository roomRepository;
  private final ObjectMapper objectMapper;

  private static final String ROOM_KEY_PREFIX = "room_cache:";
  private static final String SYNC_SET_KEY = "rooms_to_sync";

  public RedisGameService(
      RedisTemplate<String, Object> redisTemplate,
      StringRedisTemplate stringRedisTemplate,
      RoomRepository roomRepository,
      ObjectMapper objectMapper
  ) {
    this.redisTemplate = redisTemplate;
    this.stringRedisTemplate = stringRedisTemplate;
    this.roomRepository = roomRepository;
    this.objectMapper = objectMapper;
  }

  @CircuitBreaker(name = "redisService", fallbackMethod = "saveDirectlyToDatabase")
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

  /**
   * Fallback method when Redis is unavailable.
   * Saves move cache directly to PostgreSQL database.
   */
  @RateLimiter(name = "databaseWrites", fallbackMethod = "rejectOverload")
  @Bulkhead(name = "databaseWrites", fallbackMethod = "rejectOverload")
  public void saveDirectlyToDatabase(String code, MoveCache cache, Throwable t) {
    log.warn("Redis unavailable, saving move directly to database for room: {}. Error: {}", code, t.getMessage());

    try {
      Room room = roomRepository.findByCode(code).orElseThrow(() -> new RoomNotFoundException(code));
      room.setFen(cache.getFen());
      room.setCapturedPieces(cache.getCapturedPieces());
      room.setGameStatus(cache.getGameStatus());
      room.setLastActivity(cache.getLastActivity());
      room.setMoveSequence(cache.getMoveSequence());

      roomRepository.save(room);

      log.info("Move saved directly to database for room: {}", code);
    } catch (Exception e) {
      log.error("Failed to save move to database for room: {}. Error: {}", code, e.getMessage(), e);
      throw new RuntimeException("Failed to persist move after Redis failure", e);
    }
  }

  public void rejectOverload(String code, MoveCache cache, Throwable t) {
    log.error("Database overloaded, rejecting move save for room: {}. Reason: {}", code, t.getMessage());
    throw new RuntimeException("System overloaded. Please retry in a moment. Original error: " + t.getMessage());
  }
}