package com.mychess.my_chess_backend.exceptions.websocket;

public class MissingUsernameInWebsocket extends RuntimeException {
    private final static String message = "Missing username in WebSocket session attributes";

    public MissingUsernameInWebsocket() {
        super(message);
    }
}
