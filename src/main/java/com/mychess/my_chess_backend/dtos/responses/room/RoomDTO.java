package com.mychess.my_chess_backend.dtos.responses.room;

import com.mychess.my_chess_backend.dtos.responses.auth.AuthenticatedUserDTO;
import com.mychess.my_chess_backend.utils.enums.GameStatus;
import com.mychess.my_chess_backend.utils.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class RoomDTO {
    private UUID id;
    private RoomStatus roomStatus;
    private GameStatus gameStatus;
    private String code;
    private LocalDateTime lastActivity;
    private AuthenticatedUserDTO whitePlayer;
    private AuthenticatedUserDTO blackPlayer;
}
