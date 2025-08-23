package com.mychess.my_chess_backend.responses.auth;

import com.mychess.my_chess_backend.models.User;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class LoginResponse {
    private User user;
}
