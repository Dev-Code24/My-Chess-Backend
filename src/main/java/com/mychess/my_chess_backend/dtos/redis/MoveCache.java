package com.mychess.my_chess_backend.dtos.redis;

import com.mychess.my_chess_backend.utils.enums.GameStatus;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MoveCache {
  private String fen;
  private String capturedPieces;
  private UUID whitePlayerId;
  private UUID blackPlayerId;
  private GameStatus gameStatus;
  private LocalDateTime lastActivity;
  private Long moveSequence;
}
