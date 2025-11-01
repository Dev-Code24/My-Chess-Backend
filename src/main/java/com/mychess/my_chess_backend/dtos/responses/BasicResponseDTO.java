package com.mychess.my_chess_backend.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class BasicResponseDTO<Data> {
    private final Instant timestamp = Instant.now();
    private String message;
    private int status;
    private Data data;
    private String selfLink;
}
