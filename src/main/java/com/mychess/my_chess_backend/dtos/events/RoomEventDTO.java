package com.mychess.my_chess_backend.dtos.events;

import com.mychess.my_chess_backend.utils.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for Redis Pub/Sub room events.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomEventDTO implements Serializable {
  private String roomCode;
  private String serverId;
  private EventType eventType;
  private Object payload;
  private Long timestamp;


  public RoomEventDTO(String roomCode, String serverId, EventType eventType, Object payload) {
    this.roomCode = roomCode;
    this.serverId = serverId;
    this.eventType = eventType;
    this.payload = payload;
    this.timestamp = System.currentTimeMillis();
  }
}
