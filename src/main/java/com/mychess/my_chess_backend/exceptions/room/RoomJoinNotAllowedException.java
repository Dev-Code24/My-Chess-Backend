package com.mychess.my_chess_backend.exceptions.room;

public class RoomJoinNotAllowedException extends RuntimeException {
    public RoomJoinNotAllowedException(String message) {
        super(message);
    }
}