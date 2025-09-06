package com.mychess.my_chess_backend.dtos.requests.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticatingUserDTO {
    private String email, password;
}
