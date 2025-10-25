package com.mychess.my_chess_backend.dtos.responses.room;

import com.mychess.my_chess_backend.dtos.shared.Move;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class PieceMovedResponseDTO {
    private Move move;
    private String fen;
}
