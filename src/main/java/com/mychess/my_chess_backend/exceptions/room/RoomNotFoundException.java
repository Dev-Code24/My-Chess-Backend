package com.mychess.my_chess_backend.exceptions.room;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String code) {
        super("Room with code " + code + " not found.");
    }
}