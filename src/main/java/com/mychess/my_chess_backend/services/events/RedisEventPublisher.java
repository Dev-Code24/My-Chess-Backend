package com.mychess.my_chess_backend.services.events;

import com.mychess.my_chess_backend.dtos.events.RoomEventDTO;
import com.mychess.my_chess_backend.dtos.responses.room.RoomDTO;
import com.mychess.my_chess_backend.utils.enums.EventType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for publishing room events to Redis Pub/Sub channels.
 */
@Service
@Slf4j
public class RedisEventPublisher {
  private final RedisTemplate<String, Object> pubSubRedisTemplate;

  @Getter
  @Value("${server.instance.id:#{T(java.util.UUID).randomUUID().toString()}}")
  private String serverId;

  private static final String CHANNEL_PREFIX = "room:";
  private static final String CHANNEL_SUFFIX = ":events";

  public RedisEventPublisher(
      @Qualifier("pubSubRedisTemplate") RedisTemplate<String, Object> pubSubRedisTemplate
  ) {
    this.pubSubRedisTemplate = pubSubRedisTemplate;
  }

  public void publishRoomEvent(String roomCode, EventType eventType, Object payload) {
    try {
      String channel = CHANNEL_PREFIX + roomCode + CHANNEL_SUFFIX;
      RoomEventDTO event = new RoomEventDTO(roomCode, serverId, eventType, payload);

      pubSubRedisTemplate.convertAndSend(channel, event);
    } catch (Exception e) {
      log.error("Failed to publish {} event for room {}. Error: {}", eventType, roomCode, e.getMessage(), e);
    }
  }

  public void publishMessage(String roomCode, String message) {
    publishRoomEvent(roomCode, EventType.MESSAGE, message);
  }

  public void publishRoomUpdate(String roomCode, RoomDTO roomDTO) {
    publishRoomEvent(roomCode, EventType.ROOM_UPDATE, roomDTO);
  }
}
