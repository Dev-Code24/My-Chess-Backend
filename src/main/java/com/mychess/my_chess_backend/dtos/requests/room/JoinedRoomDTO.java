package com.mychess.my_chess_backend.dtos.requests.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class JoinedRoomDTO {
    private String roomId;
    private String whitePlayer;
    private String blackPlayer;
}
