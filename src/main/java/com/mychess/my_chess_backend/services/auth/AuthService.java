package com.mychess.my_chess_backend.services.auth;

import com.mychess.my_chess_backend.dtos.requests.auth.AuthenticatingUserDTO;
import com.mychess.my_chess_backend.dtos.requests.auth.RegisteringUserDTO;
import com.mychess.my_chess_backend.exceptions.user.UserAlreadyExistsException;
import com.mychess.my_chess_backend.exceptions.user.UserErrorMessage;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.utils.enums.AuthProvider;
import com.mychess.my_chess_backend.repositories.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public User signUp(RegisteringUserDTO user) {
        User registeredUser = this.userRepository.findByEmail(user.getEmail()).orElse(null);

        if (registeredUser != null) {
            throw new UserAlreadyExistsException(UserErrorMessage.EMAIL_ALREADY_EXISTS.getValue());
        }

        User newUser = new User()
            .setUsername(user.getUsername())
            .setEmail(user.getEmail())
            .setPassword(this.passwordEncoder.encode(user.getPassword()))
            .setIsActive(true);

        if (user.getAuthProvider() == null || user.getAuthProvider() == AuthProvider.LOCAL) {
            newUser.setAuthProvider(AuthProvider.LOCAL);
        }

        return this.userRepository.save(newUser);
    }
    public User authenticate(AuthenticatingUserDTO user) {
        this.authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
        );
        User authenticatedUser = this.userRepository.findByEmail(user.getEmail()).orElseThrow();
        authenticatedUser.setIsActive(true);
        return this.userRepository.save(authenticatedUser);
    }

    public void logout(User user) {
        user.setIsActive(false);
        user.setInGame(false);
        this.userRepository.save(user);
    }
}
