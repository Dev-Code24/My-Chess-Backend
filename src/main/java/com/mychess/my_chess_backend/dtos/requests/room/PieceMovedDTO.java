package com.mychess.my_chess_backend.dtos.requests.room;

import com.mychess.my_chess_backend.utils.enums.ChessPiece;
import com.mychess.my_chess_backend.utils.enums.ChessPieceColor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class PieceMovedDTO {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Piece {
        private String id;
        private byte col;
        private byte row;
        private ChessPieceColor color;
        private ChessPiece type;
    }

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
}
