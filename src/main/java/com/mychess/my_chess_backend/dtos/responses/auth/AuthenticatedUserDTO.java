package com.mychess.my_chess_backend.dtos.responses.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticatedUserDTO {
    private String email;
    private String username;
    private boolean inGame;
}
