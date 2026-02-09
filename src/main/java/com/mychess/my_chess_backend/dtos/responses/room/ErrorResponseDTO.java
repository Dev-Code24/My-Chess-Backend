package com.mychess.my_chess_backend.dtos.responses.room;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {
  private Instant timestamp = Instant.now();
  private String error;
  private String message;
  private int status;
  private String type;

  public ErrorResponseDTO(String message, int status, String type) {
    this.message = message;
    this.status = status;
    this.type = type;
  }
}
