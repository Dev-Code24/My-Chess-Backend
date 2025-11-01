package com.mychess.my_chess_backend.utils;

import com.mychess.my_chess_backend.dtos.shared.Move;
import com.mychess.my_chess_backend.dtos.shared.MoveDetails;
import com.mychess.my_chess_backend.dtos.shared.Piece;
import com.mychess.my_chess_backend.dtos.shared.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveUtils {

    public static List<Piece> handleMove(List<Piece> pieces, Move move) {
        MoveDetails moveDetails = move.getMoveDetails();
        Position targetPosition = move.getTo();
        if (moveDetails.getTargetPiece() != null) {
            return handlePieceCapture(pieces, moveDetails.getTargetPiece());
        } else if (moveDetails.getCastling() != null) {
           return handleCastling(pieces, targetPosition, move.getPiece(), move.getMoveDetails());
        } else if (Boolean.TRUE == moveDetails.getPromotion() && move.getMoveDetails().getPromotedPiece() != null) {
            return handlePawnPromotion(pieces, move);
        }

        return pieces;
    }

    private static List<Piece> handlePieceCapture(List<Piece> pieces, Piece targetPiece) {
        List<Piece> newPieces = new ArrayList<>(pieces);
        newPieces.removeIf(piece -> piece.getId().equals(targetPiece.getId()));
        return newPieces;
    }

    private static List<Piece> handlePawnPromotion(List<Piece> pieces, Move move) {
        List<Piece> newPieces = new ArrayList<>(pieces);
        Piece pawn = move.getPiece();
        Piece promotedPiece = move.getMoveDetails().getPromotedPiece().toBuilder().build();
        Position to = move.getTo();
        newPieces.removeIf(p -> p.getId().equals(pawn.getId()));

        byte newRow = to.getRow();
        if (pawn.getColor().equals("w")) {
            newRow = (byte) (7 - to.getRow());
        }

        promotedPiece.setRow(newRow);
        promotedPiece.setCol(to.getCol());
        newPieces.add(promotedPiece);
        return newPieces;
    }

    private static List<Piece> handleCastling(
            List<Piece> pieces,
            Position targetPosition,
            Piece king,
            MoveDetails moveDetails
    ) {
        List<Piece> newPieces = new ArrayList<>(pieces);
        int rowDiff = targetPosition.getRow() - king.getRow();
        int colDiff = targetPosition.getCol() - king.getCol();
        // Castling detected: same row, 2-col move
        if (rowDiff == 0 && Math.abs(colDiff) == 2 && !king.isHasMoved()) {
            boolean kingside = colDiff > 0;
            int rookStartCol = kingside ? 7 : 0;
            int rookTargetCol = kingside ? 5 : 3;

            // Find rook
            Piece rook = newPieces.stream()
                    .filter(p -> "rook".equals(p.getType())
                            && p.getColor().equals(king.getColor())
                            && p.getRow() ==  king.getRow() - 7
                            && p.getCol() == rookStartCol
                            && !p.isHasMoved())
                    .findFirst()
                    .orElse(null);

            if (rook != null) {
                boolean pathClear = true;
                int colStep = kingside ? 1 : -1;
                for (int c = king.getCol() + colStep; c != rookStartCol; c += colStep) {
                    final int col = c;
                    if (newPieces.stream().anyMatch(p -> p.getRow() == king.getRow() && p.getCol() == col)) {
                        pathClear = false;
                        break;
                    }
                }

                if (pathClear) {
                    rook.setCol((byte) rookTargetCol);
                    rook.setHasMoved(true);
                    king.setHasMoved(true);
                    moveDetails.setCastling(kingside ? "kingside" : "queenside");
                } else {
                    System.out.println("Castling failed: path blocked");
                }
            } else {
                System.out.println("Castling failed: no valid rook found");
            }
        }

        return newPieces;
    }
}
