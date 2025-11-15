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

    public String getUsernameById(UUID id) {
        return this.userRepository.findById(id).map(User::getUsername).orElse(null);
    }

    public User getUserById(UUID id) {
        return this.userRepository.findById(id).orElse(null);
    }

    public User getUserByEmail(String email) {
        return this.userRepository.findByEmail(email).orElse(null);
    }

    public boolean updateUser(User user) {
        try {
            this.userRepository.save(user);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
