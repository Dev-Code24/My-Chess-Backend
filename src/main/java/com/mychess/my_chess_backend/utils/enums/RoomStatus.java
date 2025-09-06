package com.mychess.my_chess_backend.utils.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RoomStatus {
    AVAILABLE("available"),
    OCCUPIED("occupied"),
    INACTIVE("inactive");

    private final String value;
    RoomStatus(String value) { this.value = value; }
    @JsonValue
    public String getValue() { return this.value; }
}
