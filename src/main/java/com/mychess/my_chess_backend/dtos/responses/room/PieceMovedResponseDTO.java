package com.mychess.my_chess_backend.dtos.responses.room;

import com.mychess.my_chess_backend.dtos.shared.Move;
import com.mychess.my_chess_backend.dtos.shared.PieceMoved;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PieceMovedResponseDTO extends PieceMoved {
    private Move move;
}
