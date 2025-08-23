package com.mychess.my_chess_backend.dtos.requests.auth;

import com.mychess.my_chess_backend.utils.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class RegisteringUserDTO {
    private String email, password, username;
    private AuthProvider authProvider;
}
