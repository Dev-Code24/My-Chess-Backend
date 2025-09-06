package com.mychess.my_chess_backend.utils.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GameStatus {
    WAITING("waiting"),
    IN_PROGRESS("in_progress"),
    DRAW("draw"),
    CANCELLED("cancelled"),
    WHITE_WON("white_won"),
    BLACK_WON("black_won");

    private final String value;
    GameStatus(String value) { this.value = value; }
    @JsonValue
    public String getValue() { return this.value; }
}
