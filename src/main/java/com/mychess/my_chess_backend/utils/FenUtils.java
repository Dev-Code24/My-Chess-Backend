package com.mychess.my_chess_backend.utils;

import com.mychess.my_chess_backend.dtos.shared.Piece;
import com.mychess.my_chess_backend.utils.enums.ChessPiece;
import com.mychess.my_chess_backend.utils.enums.ChessPieceColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FenUtils {
    public static List<Piece> parseFenToPieces(String fen) {
        List<Piece> pieces = new ArrayList<>();
        String[] parts = fen.split(" ");
        String[] rows = parts[0].split("/");

        for (int row = 0; row < 8; row++) {
            int col = 0;
            for (char c : rows[row].toCharArray()) {
                if (Character.isDigit(c)) {
                    col += Character.getNumericValue(c);
                } else {
                    String color = Character.isUpperCase(c) ?
                            ChessPieceColor.WHITE.getValue() :
                            ChessPieceColor.BLACK.getValue();
                    String type = ChessPiece.fromFenChar(Character.toLowerCase(c)).getValue();
                    pieces.add(new Piece(
                            color + "-" + type + "-" + col,
                            (byte) col,
                            (byte) (7 - row),
                            color,
                            type,
                            false,
                            false
                    ));
                    col++;
                }
            }
        }
        return pieces;
    }

    public static String piecesToFen(List<Piece> pieces, String turn) {
        char[][] board = new char[8][8];
        for (char[] row : board) Arrays.fill(row, ' ');

        for (Piece piece : pieces) {
            int fenRow = 7 - piece.getRow();
            char symbol = ChessPiece.toFenChar(piece.getType(), piece.getColor());
            board[fenRow][piece.getCol()] = symbol;
        }

        StringBuilder fen = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            int empty = 0;
            for (int col = 0; col < 8; col++) {
                char c = board[row][col];
                if (c == ' ') empty++;
                else {
                    if (empty > 0) { fen.append(empty); empty = 0; }
                    fen.append(c);
                }
            }
            if (empty > 0) fen.append(empty);
            if (row < 7) fen.append('/');
        }
        fen.append(" ").append(turn).append(" ").append("KQkq - 0 1");
        return fen.toString();
    }

    public static String getTurn(String fen) {
        String[] parts = fen.split(" ");
        return parts[1];
    }

    public static String getNextTurn(String fen) {
        return Objects.equals(getTurn(fen), "w") ? "b" : "w";
    }
}