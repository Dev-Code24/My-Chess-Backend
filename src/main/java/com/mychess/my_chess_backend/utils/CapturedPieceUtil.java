package com.mychess.my_chess_backend.utils;

import com.mychess.my_chess_backend.dtos.shared.Piece;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CapturedPieceUtil {

    public static String recordCapture(String capturedString, Piece capturedPiece) {
        String capturedPieceType = capturedPiece.getType();
        String capturedPieceColor = capturedPiece.getColor();

        String[] parts = capturedString.split("/");
        String blackSection = parts[0];
        String whiteSection = parts[1];

        if (capturedPieceColor.equalsIgnoreCase("b")) {
            blackSection = increment(blackSection, capturedPieceType.toLowerCase().charAt(0));
        } else {
            whiteSection = increment(whiteSection, Character.toUpperCase(capturedPieceType.charAt(0)));
        }

        return blackSection + "/" + whiteSection;
    }

    private static String increment(String section, char targetPiece) {
        Map<Character, Integer> counts = parseSection(section);
        counts.put(targetPiece, counts.getOrDefault(targetPiece, 0) + 1);
        return buildSection(counts);
    }

    private static Map<Character, Integer> parseSection(String section) {
        Map<Character, Integer> map = new HashMap<>();
        if (section == null || section.isEmpty()) return map;

        StringBuilder number = new StringBuilder();
        char currentPiece = 0;

        for (int i = 0; i < section.length(); i++) {
            char c = section.charAt(i);
            if (Character.isLetter(c)) {
                // Save previous piece if any
                if (currentPiece != 0 && !number.isEmpty()) {
                    map.put(currentPiece, Integer.parseInt(number.toString()));
                    number.setLength(0);
                }
                currentPiece = c;
            } else if (Character.isDigit(c)) {
                number.append(c);
            }
        }

        if (currentPiece != 0 && !number.isEmpty()) {
            map.put(currentPiece, Integer.parseInt(number.toString()));
        }

        return map;
    }


    private static String buildSection(Map<Character, Integer> counts) {
        return counts.entrySet().stream()
                .map(e -> e.getKey().toString() + e.getValue())
                .collect(Collectors.joining());
    }
}

