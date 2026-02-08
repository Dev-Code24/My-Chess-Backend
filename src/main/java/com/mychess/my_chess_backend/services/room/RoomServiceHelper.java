package com.mychess.my_chess_backend.services.room;

import com.mychess.my_chess_backend.dtos.responses.auth.AuthenticatedUserDTO;
import com.mychess.my_chess_backend.dtos.responses.room.RoomDTO;
import com.mychess.my_chess_backend.dtos.shared.Move;
import com.mychess.my_chess_backend.dtos.shared.Piece;
import com.mychess.my_chess_backend.dtos.shared.Position;
import com.mychess.my_chess_backend.models.Room;
import com.mychess.my_chess_backend.utils.FenUtils;

import java.util.ArrayList;
import java.util.List;

public class RoomServiceHelper {
    protected static boolean isCheckMate(Move move) {
        Piece targetPiece = move.getMoveDetails().getTargetPiece();
        return targetPiece != null && targetPiece.getType().equals("king");
    }

    protected static List<Piece> updateMovedPieceInList(List<Piece> pieces, Piece movedPiece, Position targetPosition) {
        List<Piece> updatedPieces = new ArrayList<>(pieces);
        for (Piece piece : updatedPieces) {
            if (piece.getId().equals(movedPiece.getId())) {
                byte finalTargetRow = targetPosition.getRow();
                if (piece.getColor().equals("w")) {
                    finalTargetRow = (byte) (7 - targetPosition.getRow());
                }
                piece.setRow(finalTargetRow);
                piece.setCol(targetPosition.getCol());
                piece.setHasMoved(true);
                piece.setEnPassantAvailable(movedPiece.getEnPassantAvailable());
                break;
            }
        }
        return updatedPieces;
    }

    protected static String getNextTurn(String fen, Move move) {
        String nextTurn = FenUtils.getNextTurn(fen);
        if (
            move.getMoveDetails().getPromotion() == Boolean.TRUE &&
            move.getMoveDetails().getPromotedPiece() != null
        ) {
            nextTurn = FenUtils.getTurn(fen);
        }

        return nextTurn;
    }

    protected RoomDTO getRoomDto(Room room, AuthenticatedUserDTO whitePlayerDTO, AuthenticatedUserDTO blackPlayerDTO) {
        return new RoomDTO()
            .setCode(room.getCode())
            .setCapturedPieces(room.getCapturedPieces())
            .setFen(room.getFen())
            .setBlackPlayer(blackPlayerDTO)
            .setWhitePlayer(whitePlayerDTO)
            .setId(room.getId())
            .setLastActivity(room.getLastActivity())
            .setRoomStatus(room.getRoomStatus())
            .setGameStatus(room.getGameStatus())
            .setMoveSequence(room.getMoveSequence());
    }
}
