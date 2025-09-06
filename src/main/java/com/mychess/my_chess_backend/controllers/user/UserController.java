package com.mychess.my_chess_backend.controllers.user;

import com.mychess.my_chess_backend.dtos.responses.BasicResponseDTO;
import com.mychess.my_chess_backend.dtos.responses.auth.AuthenticatedUserDTO;
import com.mychess.my_chess_backend.models.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/user")
@RestController
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<BasicResponseDTO<AuthenticatedUserDTO>> getUser(HttpServletRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(new BasicResponseDTO<>(
                "success",
                HttpStatus.OK.value(),
                new AuthenticatedUserDTO(user.getUsername(), user.getEmail()),
                req.getRequestURI()
                )
        );
    }
}
