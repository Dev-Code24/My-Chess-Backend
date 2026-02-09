package com.mychess.my_chess_backend.exceptions.room;

public class StaleMoveException extends RuntimeException {
  public StaleMoveException(String message) {
    super(message);
  }

  public StaleMoveException(Long expected, Long actual) {
    super(String.format("Stale move detected. Expected sequence: %d, but current is: %d", expected, actual));
  }
}
