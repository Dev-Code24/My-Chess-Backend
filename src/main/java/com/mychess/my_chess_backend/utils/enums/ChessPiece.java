package com.mychess.my_chess_backend.utils.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChessPiece {
    PAWN("pawn"),
    ROOK("rook"),
    KNIGHT("knight"),
    BISHOP("bishop"),
    QUEEN("queen"),
    KING("king");

    private final String value;
    ChessPiece(String value) { this.value = value; }
    @JsonValue
    public String getValue() { return this.value; }
}
