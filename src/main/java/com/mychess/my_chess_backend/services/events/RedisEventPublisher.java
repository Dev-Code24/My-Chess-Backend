package com.mychess.my_chess_backend.services.events;

import com.mychess.my_chess_backend.dtos.events.MoveEventDTO;
import com.mychess.my_chess_backend.dtos.responses.room.PieceMovedResponseDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for publishing move events to Redis Pub/Sub channels.
 * Enables cross-server communication for WebSocket broadcasts in a distributed environment.
 */
@Service
@Slf4j
public class RedisEventPublisher {
  private final RedisTemplate<String, Object> pubSubRedisTemplate;

  /**
   * Unique identifier for this server instance.
   * Generated automatically if not provided via environment variable.
   */
  @Getter
  @Value("${server.instance.id:#{T(java.util.UUID).randomUUID().toString()}}")
  private String serverId;

  private static final String CHANNEL_PREFIX = "room:";
  private static final String CHANNEL_SUFFIX = ":moves";

  public RedisEventPublisher(@Qualifier("pubSubRedisTemplate") RedisTemplate<String, Object> pubSubRedisTemplate) {
    this.pubSubRedisTemplate = pubSubRedisTemplate;
  }

  public void publishMoveEvent(String roomCode, PieceMovedResponseDTO moveData) {
    try {
      String channel = CHANNEL_PREFIX + roomCode + CHANNEL_SUFFIX;
      MoveEventDTO event = new MoveEventDTO(roomCode, serverId, moveData);

      pubSubRedisTemplate.convertAndSend(channel, event);

      log.debug("Published move event to channel {} from server {}", channel, serverId);
    } catch (Exception e) {
      log.error("Failed to publish move event for room {}. Error: {}", roomCode, e.getMessage(), e);
    }
  }
}
