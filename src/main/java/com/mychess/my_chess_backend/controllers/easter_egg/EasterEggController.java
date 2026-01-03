package com.mychess.my_chess_backend.controllers.easter_egg;

import com.mychess.my_chess_backend.dtos.responses.BasicResponseDTO;
import com.mychess.my_chess_backend.models.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EasterEggController {

    @GetMapping("/hello-world")
    public ResponseEntity<BasicResponseDTO<String>> helloWorld(
        HttpServletRequest req
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        StringBuilder sb = new StringBuilder("Hello ");

        if (authentication != null &&
            authentication.isAuthenticated() &&
            authentication.getPrincipal() instanceof User
        ) {
            sb.append(((User) authentication.getPrincipal()).getUsername()).append(" !");
        } else {
            sb.append("World !");
        }
        return ResponseEntity.ok(new BasicResponseDTO<>(
            "success",
            HttpStatus.OK.value(),
            sb.toString(),
            req.getRequestURI()
        ));
    }
}
