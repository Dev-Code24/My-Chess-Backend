package com.mychess.my_chess_backend.utils.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChessPieceColor {
    WHITE("w"),
    BLACK("b");

    private final String value;
    ChessPieceColor(String value) { this.value = value; }
    @JsonValue
    public String getValue() { return this.value; }
}
