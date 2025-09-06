package com.mychess.my_chess_backend.services.user;

import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getUsernameById(UUID userId) {
        Optional<User> user = this.userRepository.findById(userId);
        return user.map(User::getUsername).orElse(null);
    }
}
