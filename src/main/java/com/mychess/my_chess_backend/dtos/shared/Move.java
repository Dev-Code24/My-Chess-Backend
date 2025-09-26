package com.mychess.my_chess_backend.dtos.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Move {
    private boolean valid;
    private Optional<Piece> capture;
    private Optional<Boolean> promotion;
    private Optional<String> castling;
    private Optional<String> reason;
    private Optional<Boolean> enPassant;
}
