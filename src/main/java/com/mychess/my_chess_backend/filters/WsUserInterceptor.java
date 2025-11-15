package com.mychess.my_chess_backend.filters;

import com.mychess.my_chess_backend.exceptions.websocket.MissingUsernameInWebsocket;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.services.user.UserService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
public class WsUserInterceptor implements ChannelInterceptor {
    private final UserService userService;

    public WsUserInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }
        if (command == StompCommand.CONNECT) {
            if (accessor.getSessionAttributes() == null) {
                throw new MissingUsernameInWebsocket();
            }

            Object emailObj = accessor.getSessionAttributes().get("email");
            if (emailObj == null) {
                throw new MissingUsernameInWebsocket();
            }

            String email = (String) emailObj;
            User user = userService.getUserByEmail(email);

            if (user == null) {
                throw new IllegalStateException("User " + email + " not found for websocket");
            }
            accessor.setUser(user);
        }

        return message;
    }
}
