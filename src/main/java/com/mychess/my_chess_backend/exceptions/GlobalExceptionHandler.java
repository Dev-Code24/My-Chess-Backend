package com.mychess.my_chess_backend.exceptions;

import com.mychess.my_chess_backend.dtos.responses.BasicResponseDTO;
import com.mychess.my_chess_backend.exceptions.room.RoomJoinNotAllowedException;
import com.mychess.my_chess_backend.exceptions.room.RoomNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<BasicResponseDTO<Void>> handleRoomNotFound(
            RoomNotFoundException exception,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new BasicResponseDTO<>(
                        exception.getMessage(),
                        HttpStatus.NOT_FOUND.value(),
                        null,
                        req.getRequestURI()
                )
        );
    }

    @ExceptionHandler(RoomJoinNotAllowedException.class)
    public ResponseEntity<BasicResponseDTO<Void>> handleRoomJoinNotAllowed(
            RoomJoinNotAllowedException exception,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new BasicResponseDTO<>(
                        exception.getMessage(),
                        HttpStatus.FORBIDDEN.value(),
                        null,
                        req.getRequestURI()
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BasicResponseDTO<Void>> handleGeneral(
            Exception exception,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new BasicResponseDTO<>(
                        exception.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        null,
                        req.getRequestURI()
                )
        );
    }
}
