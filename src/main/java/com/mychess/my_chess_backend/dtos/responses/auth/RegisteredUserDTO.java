package com.mychess.my_chess_backend.dtos.responses.auth;

import com.mychess.my_chess_backend.utils.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RegisteredUserDTO {
    private String email;
    private String username;
}
