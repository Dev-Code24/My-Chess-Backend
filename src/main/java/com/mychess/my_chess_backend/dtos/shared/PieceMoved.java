package com.mychess.my_chess_backend.dtos.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PieceMoved {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Position {
        private byte row;
        private byte col;
    }

    private Piece piece;
    private Position to;
    private Piece targetPiece;
    private Move move;
}
