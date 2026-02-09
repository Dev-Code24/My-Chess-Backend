package com.mychess.my_chess_backend.exceptions.room;

public class SystemOverloadException extends RuntimeException {
  public SystemOverloadException(String message) {
    super(message);
  }

  public SystemOverloadException(String message, Throwable cause) {
    super(message, cause);
  }
}
