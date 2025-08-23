package com.mychess.my_chess_backend.utils.enums;

import lombok.Getter;

@Getter
public enum Headers {
    AUTHORIZATION("Authorization");

    private final String value;
    Headers(String value) { this.value = value; }
}
