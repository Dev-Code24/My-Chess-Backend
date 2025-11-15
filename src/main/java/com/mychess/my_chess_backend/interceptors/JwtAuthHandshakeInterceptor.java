package com.mychess.my_chess_backend.interceptors;

import com.mychess.my_chess_backend.services.auth.JWTService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;

@Component
public class JwtAuthHandshakeInterceptor implements HandshakeInterceptor {
    private final JWTService jwtService;

    public JwtAuthHandshakeInterceptor(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) throws Exception {
        HttpServletRequest req = ((ServletServerHttpRequest) request).getServletRequest();
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return false;
        }

        String jwt = Arrays.stream(cookies)
                .filter(c -> "access_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (jwt == null || this.jwtService.isTokenExpired(jwt)) {
            return false;
        }

        String email = this.jwtService.extractEmail(jwt);
        attributes.put("email", email);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) { }
}
