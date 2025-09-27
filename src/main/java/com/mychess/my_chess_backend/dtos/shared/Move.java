package com.mychess.my_chess_backend.dtos.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Move {
    private boolean valid;
    private Piece capture;
    private Boolean promotion;
    private String castling;
    private String reason;
    private Boolean enPassant;
}
