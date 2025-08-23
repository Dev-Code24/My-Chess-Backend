package com.mychess.my_chess_backend.utils.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AuthProvider {
    LOCAL("local"),
    GOOGLE("google");

    private final String value;
    AuthProvider(String value) { this.value = value; }

    @JsonCreator
    public static AuthProvider fromValue(String value) throws Exception {
        for (AuthProvider provider : AuthProvider.values()) {
            if (provider.value.equalsIgnoreCase(value)) { return provider; }
        }
        throw new IllegalAccessException("Invalid auth provider " + value);
    }
    @JsonValue
    public String getValue() { return this.value; }
}
