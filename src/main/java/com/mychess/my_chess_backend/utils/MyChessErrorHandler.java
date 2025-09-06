package com.mychess.my_chess_backend.utils;

import com.mychess.my_chess_backend.dtos.responses.BasicResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class MyChessErrorHandler {
    public static <T> ResponseEntity<BasicResponseDTO<T>> exceptionHandler(String message, String selfLink) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new BasicResponseDTO<>(
                message,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null,
                selfLink
        ));
    }
}
