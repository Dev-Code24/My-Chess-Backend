package com.mychess.my_chess_backend.dtos.shared;

import com.mychess.my_chess_backend.utils.enums.ChessPiece;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class MoveDetails {
    private boolean valid;
    private Piece capture;
    private Boolean promotion;
    private ChessPiece promotionType;
    private String castling;
    private String situation;
    private Boolean enPassant;
}
