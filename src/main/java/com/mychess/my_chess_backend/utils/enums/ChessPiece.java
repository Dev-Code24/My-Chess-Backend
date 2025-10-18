package com.mychess.my_chess_backend.utils.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public enum ChessPiece {
    PAWN("pawn"),
    ROOK("rook"),
    KNIGHT("knight"),
    BISHOP("bishop"),
    QUEEN("queen"),
    KING("king");

    private final String value;
    ChessPiece(String value) { this.value = value; }
    @JsonValue
    public String getValue() { return this.value; }

    public static ChessPiece fromFenChar(char fenChar) {
        return switch (fenChar) {
            case 'k' -> KING;
            case 'q' -> QUEEN;
            case 'r' -> ROOK;
            case 'b' -> BISHOP;
            case 'n' -> KNIGHT;
            case 'p' -> PAWN;
            default -> throw new IllegalArgumentException("Invalid FEN piece: " + fenChar);
        };
    }

    public static char toFenChar(String type, String color) {
        char c = switch (type) {
            case "king" -> 'k';
            case "queen" -> 'q';
            case "rook" -> 'r';
            case "bishop" -> 'b';
            case "knight" -> 'n';
            case "pawn" -> 'p';
            default -> throw new IllegalArgumentException("Invalid piece type " + type);
        };
        return Objects.equals(color, ChessPieceColor.WHITE.getValue()) ? Character.toUpperCase(c) : c;
    }
}
