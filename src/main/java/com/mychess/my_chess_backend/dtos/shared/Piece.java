package com.mychess.my_chess_backend.dtos.shared;

import com.mychess.my_chess_backend.utils.enums.ChessPiece;
import com.mychess.my_chess_backend.utils.enums.ChessPieceColor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Piece {
    private String id;
    private byte col;
    private byte row;
    private ChessPieceColor color;
    private ChessPiece type;
    private Boolean enPassantAvailable;
    private boolean hasMoved;
}
