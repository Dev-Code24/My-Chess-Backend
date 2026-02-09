package com.mychess.my_chess_backend.dtos.events;

import com.mychess.my_chess_backend.dtos.responses.room.PieceMovedResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Event DTO for broadcasting move events across multiple server instances via Redis Pub/Sub.
 * Contains the move data along with metadata for distributed coordination.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoveEventDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private String roomCode;
  private String serverId;
  private PieceMovedResponseDTO moveData;
  private Long timestamp;

  public MoveEventDTO(String roomCode, String serverId, PieceMovedResponseDTO moveData) {
    this.roomCode = roomCode;
    this.serverId = serverId;
    this.moveData = moveData;
    this.timestamp = System.currentTimeMillis();
  }
}
