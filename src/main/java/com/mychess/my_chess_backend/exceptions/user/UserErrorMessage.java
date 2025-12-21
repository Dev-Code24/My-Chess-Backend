package com.mychess.my_chess_backend.exceptions.user;

import lombok.Getter;

@Getter
public enum UserErrorMessage {
  EMAIL_ALREADY_EXISTS("This email already is in use.");

  private final String value;
  UserErrorMessage(String value) { this.value = value; }
}
