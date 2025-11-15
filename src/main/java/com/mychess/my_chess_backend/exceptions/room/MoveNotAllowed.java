package com.mychess.my_chess_backend.exceptions.room;

public class MoveNotAllowed extends RuntimeException {
    public MoveNotAllowed(String message) {
        super(message);
    }
}
