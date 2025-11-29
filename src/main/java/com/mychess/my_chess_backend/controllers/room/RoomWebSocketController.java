package com.mychess.my_chess_backend.controllers.room;

import com.mychess.my_chess_backend.dtos.shared.Move;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.services.room.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class RoomWebSocketController {
    private final RoomService roomService;

    public RoomWebSocketController(RoomService roomService) {
        this.roomService = roomService;
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
}
