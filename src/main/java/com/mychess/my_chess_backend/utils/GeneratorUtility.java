package com.mychess.my_chess_backend.utils;

import java.util.Random;

public class GeneratorUtility {
    private static final int MAX_LENGTH = 20;
    private static final Random RANDOM = new Random();

    public static String createUsernameFromEmail(String email) {
        String username = email.split("@")[0];
        username = username.replaceAll("[^a-zA-Z0-9]", "");
        username = username.toLowerCase();

        if (username.length() > MAX_LENGTH) {
            username = username.substring(0, MAX_LENGTH);
        }

        if (username.isEmpty()) {
            username = "user" + RANDOM.nextInt(10000);
        }

        return username;
    }
}
