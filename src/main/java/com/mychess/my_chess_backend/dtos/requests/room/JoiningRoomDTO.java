package com.mychess.my_chess_backend.dtos.requests.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoiningRoomDTO {
    private String roomId;
}
