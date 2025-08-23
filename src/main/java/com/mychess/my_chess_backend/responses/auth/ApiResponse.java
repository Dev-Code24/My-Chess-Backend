package com.mychess.my_chess_backend.responses.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class ApiResponse<Data> {
    private Data data;
    private String selfLink;
}
