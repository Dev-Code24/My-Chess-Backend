package com.mychess.my_chess_backend.services.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychess.my_chess_backend.dtos.events.MoveEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for subscribing to Redis Pub/Sub channels and forwarding move events
 * to local WebSocket subscribers. Enables cross-server real-time communication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventSubscriber implements MessageListener {
  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper;

  /**
   * Unique identifier for this server instance.
   * Used to filter out events published by this server (already broadcasted locally).
   */
  @Value("${server.instance.id:#{T(java.util.UUID).randomUUID().toString()}}")
  private String serverId;

  private static final String TOPIC_PREFIX = "/topic/room.";

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      String messageBody = new String(message.getBody());
      MoveEventDTO event = objectMapper.readValue(messageBody, MoveEventDTO.class);

      if (event.getServerId().equals(serverId)) {
        log.trace("Ignoring own message from server {} for room {}", serverId, event.getRoomCode());
        return;
      }

      String destination = TOPIC_PREFIX + event.getRoomCode();
      messagingTemplate.convertAndSend(destination, event.getMoveData());

      log.debug("Received and broadcasted move from server {} to local subscribers for room {}",
          event.getServerId(), event.getRoomCode());

    } catch (Exception e) {
      log.error("Error processing Redis Pub/Sub message: {}", e.getMessage(), e);
    }
  }
}
