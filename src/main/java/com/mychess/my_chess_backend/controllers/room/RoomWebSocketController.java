package com.mychess.my_chess_backend.controllers.room;

import com.mychess.my_chess_backend.dtos.responses.room.ErrorResponseDTO;
import com.mychess.my_chess_backend.dtos.shared.Move;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.services.room.RoomService;
import com.mychess.my_chess_backend.services.websocket.WebSocketErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket controller for handling real-time room interactions.
 */
@Controller
@Slf4j
public class RoomWebSocketController {
    private final RoomService roomService;
    private final WebSocketErrorHandler errorHandler;

    public RoomWebSocketController(RoomService roomService, WebSocketErrorHandler errorHandler) {
        this.roomService = roomService;
        this.errorHandler = errorHandler;
    }

    @MessageMapping("/room/{code}/move")
    public void move(
        @DestinationVariable String code,
        @Payload Move move,
        Principal userPrincipal
    ) {
        Authentication auth = (Authentication) userPrincipal;
        User user = (User) auth.getPrincipal();
        this.roomService.processPlayerMove(move, code, user);
    }

    @MessageMapping("/room/{code}/join")
    public void joinRoom(
        @DestinationVariable String code,
        Principal userPrincipal
    ) {
        Authentication auth = (Authentication) userPrincipal;
        User user = (User) auth.getPrincipal();
        this.roomService.handlePlayerJoinRoom(code, user);
    }

    /**
     * Centralized exception handler for all WebSocket errors.
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ErrorResponseDTO handleWebSocketException(Exception exception, Principal userPrincipal) {
        return this.errorHandler.handleException(exception, userPrincipal);
    }
}
