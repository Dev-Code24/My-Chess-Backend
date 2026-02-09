package com.mychess.my_chess_backend.services.websocket;

import com.mychess.my_chess_backend.dtos.responses.room.ErrorResponseDTO;
import com.mychess.my_chess_backend.exceptions.room.MoveNotAllowed;
import com.mychess.my_chess_backend.exceptions.room.StaleMoveException;
import com.mychess.my_chess_backend.exceptions.room.SystemOverloadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.Principal;

/**
 * Centralized service for handling WebSocket-related errors and exceptions.
 */
@Service
@Slf4j
public class WebSocketErrorHandler {

  public ErrorResponseDTO handleException(Exception exception, Principal userPrincipal) {
    String username = userPrincipal != null ? userPrincipal.getName() : "unknown";

    return switch (exception) {
      case SystemOverloadException systemOverloadException -> handleSystemOverload(systemOverloadException, username);
      case MoveNotAllowed moveNotAllowed -> handleMoveNotAllowed(moveNotAllowed, username);
      case StaleMoveException staleMoveException -> handleStaleMove(staleMoveException, username);
      case null, default -> handleGenericException(exception, username);
    };
  }

  private ErrorResponseDTO handleSystemOverload(SystemOverloadException exception, String username) {
    log.error("System overload exception for user {}: {}", username, exception.getMessage());
    return new ErrorResponseDTO(
        exception.getMessage(),
        503,
        "SYSTEM_OVERLOAD"
    );
  }

  private ErrorResponseDTO handleMoveNotAllowed(MoveNotAllowed exception, String username) {
    log.warn("Move not allowed for user {}: {}", username, exception.getMessage());
    return new ErrorResponseDTO(
        exception.getMessage(),
        400,
        "MOVE_NOT_ALLOWED"
    );
  }

  private ErrorResponseDTO handleStaleMove(StaleMoveException exception, String username) {
    log.warn("Stale move for user {}: {}", username, exception.getMessage());
    return new ErrorResponseDTO(
        exception.getMessage(),
        409,
        "STALE_MOVE"
    );
  }

  private ErrorResponseDTO handleGenericException(Exception exception, String username) {
    log.error("Unexpected error for user {}: {}", username, exception.getMessage(), exception);
    return new ErrorResponseDTO(
        "An unexpected error occurred. Please try again.",
        500,
        "INTERNAL_ERROR"
    );
  }
}
