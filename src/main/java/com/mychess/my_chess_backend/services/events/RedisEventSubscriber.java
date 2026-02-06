package com.mychess.my_chess_backend.services.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychess.my_chess_backend.dtos.events.RoomEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for subscribing to Redis Pub/Sub channels and forwarding room events to local WebSocket subscribers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventSubscriber implements MessageListener {
  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper;

  @Value("${server.instance.id:#{T(java.util.UUID).randomUUID().toString()}}")
  private String serverId;

  private static final String TOPIC_PREFIX = "/topic/room.";

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      String messageBody = new String(message.getBody());
      RoomEventDTO event = objectMapper.readValue(messageBody, RoomEventDTO.class);

      if (event.getServerId().equals(serverId)) { return; }

      String destination = TOPIC_PREFIX + event.getRoomCode();
      messagingTemplate.convertAndSend(destination, event.getPayload());
    } catch (Exception e) {
      log.error("Error processing Redis Pub/Sub message: {}", e.getMessage(), e);
    }
  }
}
