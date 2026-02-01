package com.mychess.my_chess_backend.exceptions.room;

import lombok.Getter;

@Getter
public enum RoomErrorMessage {
    ALREADY_IN_ROOM("You are already in another room."),
    CANNOT_JOIN_YOUR_OWN_ROOM("You cannot join your own room as opponent."),
    ROOM_ALREADY_FULL("This room is already full."),
    UNAUTHORIZED_MOVE("Unauthorized move."),
    GAME_INACTIVE("Game is not active."),
    WHITES_TURN("It's White's turn."),
    BLACKS_TURN("It's Black's turn."),
    STALE_MOVE("This move has already been processed or is outdated.");

    private final String value;
    RoomErrorMessage(String value) { this.value = value; }
}
