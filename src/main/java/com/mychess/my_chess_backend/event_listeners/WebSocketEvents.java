package com.mychess.my_chess_backend.event_listeners;

import com.mychess.my_chess_backend.models.Room;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.services.room.RoomService;
import com.mychess.my_chess_backend.services.user.UserService;
import com.mychess.my_chess_backend.utils.enums.GameStatus;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketEvents {
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final RoomService roomService;

    public WebSocketEvents(
            SimpMessagingTemplate messagingTemplate,
            UserService userService,
            RoomService roomService
    ) {
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
        this.roomService = roomService;
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        User disconnectedUser = this.extractUser(event);
        if (disconnectedUser == null) {
            return;
        }

        Room room = this.roomService.getRoomByUserId(disconnectedUser.getId());

        if (room == null) {
            return;
        }

        String code = room.getCode();
        String message = "Player " + disconnectedUser.getUsername() + " disconnected.";
        messagingTemplate.convertAndSend("/topic/room." + code, message);
        disconnectedUser.setInGame(false);
        this.userService.updateUser(disconnectedUser);
        room.setGameStatus(GameStatus.PAUSED);
        this.roomService.updateRoom(room);
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        User connectedUser = this.extractUser(event);
        if (connectedUser == null) {
            return;
        }
        Room room = this.roomService.getRoomByUserId(connectedUser.getId());

        if (room == null) {
            return;
        }

        String code = room.getCode();
        String message = "Player " + connectedUser.getUsername() + " connected.";
        messagingTemplate.convertAndSend("/topic/room." + code, message);
        connectedUser.setInGame(false);
        this.userService.updateUser(connectedUser);
        room.setGameStatus(GameStatus.PAUSED);
        this.roomService.updateRoom(room);
    }

    private User extractUser(AbstractSubProtocolEvent event) {
        Principal principal = event.getUser();
        if (principal == null) {
            return null;
        }

        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            Object p = token.getPrincipal();
            if (p instanceof User user) {
                return user;
            }
        }

        return null;
    }
}
