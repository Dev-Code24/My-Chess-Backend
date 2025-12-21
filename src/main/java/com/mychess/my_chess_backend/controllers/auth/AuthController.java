package com.mychess.my_chess_backend.controllers.auth;

import com.mychess.my_chess_backend.dtos.requests.auth.AuthenticatingUserDTO;
import com.mychess.my_chess_backend.dtos.requests.auth.RegisteringUserDTO;
import com.mychess.my_chess_backend.dtos.responses.BasicResponseDTO;
import com.mychess.my_chess_backend.dtos.responses.auth.AuthenticatedUserDTO;
import com.mychess.my_chess_backend.dtos.responses.auth.RegisteredUserDTO;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.services.auth.AuthService;
import com.mychess.my_chess_backend.services.auth.JWTService;
import com.mychess.my_chess_backend.utils.GeneratorUtility;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Duration;
import java.util.function.Supplier;

@RequestMapping("/auth")
@RestController
public class AuthController {
    private final JWTService jwtService;
    private final AuthService authService;

    public AuthController(
        JWTService jwtService,
        AuthService authService
    ) {
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<BasicResponseDTO<RegisteredUserDTO>> signUp(
        @RequestBody RegisteringUserDTO user,
        HttpServletRequest req
    ) {
        // TODO: Search what is actually sent in selfLink property in production backends
        String username = user.getUsername();
        if (username == null || username.isBlank()) {
            user.setUsername(GeneratorUtility.createUsernameFromEmail(user.getEmail()));
        }

        User newUser = this.authService.signUp(user);

        return this.issueTokensAndRespond(newUser, () ->
            new BasicResponseDTO<>(
                "success",
                HttpStatus.OK.value(),
                new RegisteredUserDTO(newUser.getEmail(), newUser.getUsername()),
                req.getRequestURI()
            )
        );
    }
    @PostMapping("/login")
    public ResponseEntity<BasicResponseDTO<AuthenticatedUserDTO>> login(
        @RequestBody AuthenticatingUserDTO user,
        HttpServletRequest req
    ) {
        User authenticatedUser = this.authService.authenticate(user);
        return this.issueTokensAndRespond(authenticatedUser, () ->
            new BasicResponseDTO<>(
                "success",
                HttpStatus.OK.value(),
                new AuthenticatedUserDTO(
                    authenticatedUser.getEmail(),
                    authenticatedUser.getUsername(),
                    authenticatedUser.getInGame()),
                req.getRequestURI()
            )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<BasicResponseDTO<Void>> logout (
        HttpServletRequest req,
        Principal userPrincipal
    ) {
        Authentication auth = (Authentication) userPrincipal;
        User user = (User) auth.getPrincipal();
        this.authService.logout(user);

        ResponseCookie cookie = ResponseCookie.from("access_token", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("None")
            .maxAge(0)
            .build();

        return ResponseEntity
            .ok()
            .header("Set-Cookie", cookie.toString())
            .body(new BasicResponseDTO<>(
                "success",
                HttpStatus.OK.value(),
                null,
                req.getRequestURI()
            ));
    }

    private <T> ResponseEntity<T> issueTokensAndRespond (
        User user,
        Supplier<T> bodyFactory
    ) {
        final String jwt = this.jwtService.generateToken(user);
        ResponseCookie cookie = ResponseCookie.from("access_token", jwt)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("None")
            .maxAge(Duration.ofSeconds(this.jwtService.getJwtExpiration().toSeconds()))
            .build();
        T body = bodyFactory.get();
        return ResponseEntity
            .ok()
            .header("Set-Cookie", cookie.toString())
            .body(body);
    }
}
