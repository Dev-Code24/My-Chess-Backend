package com.mychess.my_chess_backend.services.room;

import com.mychess.my_chess_backend.dtos.responses.auth.AuthenticatedUserDTO;
import com.mychess.my_chess_backend.dtos.responses.room.PieceMovedResponseDTO;
import com.mychess.my_chess_backend.dtos.responses.room.RoomDTO;
import com.mychess.my_chess_backend.dtos.shared.Piece;
import com.mychess.my_chess_backend.dtos.shared.Move;
import com.mychess.my_chess_backend.dtos.shared.Position;
import com.mychess.my_chess_backend.exceptions.room.ErrorMessage;
import com.mychess.my_chess_backend.exceptions.room.MoveNotAllowed;
import com.mychess.my_chess_backend.models.Room;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.repositories.RoomRepository;
import com.mychess.my_chess_backend.services.user.UserService;
import com.mychess.my_chess_backend.utils.CapturedPieceUtil;
import com.mychess.my_chess_backend.utils.FenUtils;
import com.mychess.my_chess_backend.utils.MoveUtils;
import com.mychess.my_chess_backend.utils.constants.RoomConstants;
import com.mychess.my_chess_backend.utils.enums.GameStatus;
import com.mychess.my_chess_backend.utils.enums.RoomStatus;
import com.mychess.my_chess_backend.exceptions.room.RoomJoinNotAllowedException;
import com.mychess.my_chess_backend.exceptions.room.RoomNotFoundException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RoomService extends RoomServiceHelper {
    private final RoomRepository roomRepository;
    private final UserService userService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    private static final Random random = new Random();

    public RoomService(
            RoomRepository roomRepository,
            UserService userService,
            SimpMessagingTemplate simpMessagingTemplate
    ) {
        this.roomRepository = roomRepository;
        this.userService = userService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public RoomDTO createRoom(User whitePlayer) {
        // Finding existing room whitePlayer is a part of
        Room room = roomRepository.findRoomByUserId(whitePlayer.getId()).orElse(null);

        if (room == null) {
            room = Room.builder()
                    .code(generateUniqueRoomId())
                    .fen(RoomConstants.DEFAULT_CHESSBOARD_FEN)
                    .capturedPieces(RoomConstants.DEFAULT_CAPTURED_PIECES)
                    .whitePlayer(whitePlayer.getId())
                    .roomStatus(RoomStatus.AVAILABLE)
                    .gameStatus(GameStatus.WAITING)
                    .lastActivity(LocalDateTime.now())
                    .build();
            roomRepository.save(room);
        }

        if (whitePlayer.getInGame() != Boolean.TRUE) {
            whitePlayer.setInGame(true);
            userService.updateUser(whitePlayer);
        }
        return new RoomDTO().setCode(room.getCode());
    }

    public RoomDTO joinRoom(User blackPlayer, String code) {
        Room existingRoom = roomRepository.findRoomByUserId(blackPlayer.getId()).orElse(null);
        Room room = roomRepository.findByCode(code).orElseThrow(() -> new RoomNotFoundException(code));

        if (existingRoom != null && !existingRoom.getCode().equals(room.getCode())) {
            throw new RoomJoinNotAllowedException(ErrorMessage.ALREADY_IN_ROOM.getValue());
        }

        if (room.getWhitePlayer().equals(blackPlayer.getId())) {
            throw new RoomJoinNotAllowedException(ErrorMessage.CANNOT_JOIN_YOUR_OWN_ROOM.getValue());
        }

        if (room.getBlackPlayer() != null && !room.getBlackPlayer().equals(blackPlayer.getId())) {
            throw new RoomJoinNotAllowedException(ErrorMessage.ROOM_ALREADY_FULL.getValue());
        }

        blackPlayer.setInGame(true);
        room.setBlackPlayer(blackPlayer.getId()).setRoomStatus(RoomStatus.OCCUPIED).setGameStatus(GameStatus.IN_PROGRESS);
        room.setGameStatus(GameStatus.IN_PROGRESS);
        this.userService.updateUser(blackPlayer);

        RoomDTO roomDto = new RoomDTO().setCode(room.getCode());
        this.roomRepository.save(room);
        this.broadcastRoomUpdate(code, "Opponent Joined !");
        return roomDto;
    }

    public RoomDTO getRoom(String code) {
        Room room = this.roomRepository.findByCode(code).orElse(null);
        if (room == null) {
            throw new RoomNotFoundException(code);
        }

        AuthenticatedUserDTO whitePlayerDTO = null, blackPlayerDTO = null;

        if (room.getWhitePlayer() != null) {
            User whitePlayer = this.userService.getUserById(room.getWhitePlayer());
            whitePlayerDTO = new AuthenticatedUserDTO()
                    .setEmail(whitePlayer.getEmail())
                    .setUsername(whitePlayer.getUsername())
                    .setInGame(whitePlayer.getInGame());
        }
        if (room.getBlackPlayer() != null) {
            User blackPlayer = this.userService.getUserById(room.getBlackPlayer());
            blackPlayerDTO = new AuthenticatedUserDTO()
                    .setEmail(blackPlayer.getEmail())
                    .setUsername(blackPlayer.getUsername())
                    .setInGame(blackPlayer.getInGame());
        }
        return new RoomDTO()
                .setId(room.getId())
                .setCode(room.getCode())
                .setCapturedPieces(room.getCapturedPieces())
                .setFen(room.getFen())
                .setRoomStatus(room.getRoomStatus())
                .setGameStatus(room.getGameStatus())
                .setLastActivity(room.getLastActivity())
                .setWhitePlayer(whitePlayerDTO)
                .setBlackPlayer(blackPlayerDTO);
    }

    public void processPlayerMove(
            Move move,
            String code,
            User player
    ) {
        Room room = this.roomRepository.findByCode(code).orElseThrow(() -> new RoomNotFoundException(code));
        if (
                !Objects.equals(room.getWhitePlayer(), player.getId()) &&
                !Objects.equals(room.getBlackPlayer(), player.getId())
        ) {
            throw new RoomJoinNotAllowedException(ErrorMessage.UNAUTHORIZED_MOVE.getValue());
        }

        if (room.getGameStatus() != GameStatus.IN_PROGRESS) {
            throw new RoomJoinNotAllowedException(ErrorMessage.GAME_INACTIVE.getValue());
        }

        boolean isWhiteTurn = FenUtils.getTurn(room.getFen()).equals("w");
        boolean isPlayerWhite = Objects.equals(room.getWhitePlayer(), player.getId());

        if (isWhiteTurn && !isPlayerWhite && move.getMoveDetails().getPromotion() != Boolean.TRUE) {
            throw new MoveNotAllowed(ErrorMessage.WHITES_TURN.getValue());
        }

        if (!isWhiteTurn && isPlayerWhite && move.getMoveDetails().getPromotion() != Boolean.TRUE) {
            throw new MoveNotAllowed(ErrorMessage.BLACKS_TURN.getValue());
        }

        this.handleMove(move, room);
    }

    public void handleMove(Move move, Room room) {
        Piece movedPiece = move.getPiece();
        Piece targetPiece = move.getMoveDetails().getTargetPiece();
        Position targetPosition = move.getTo();
        String fen = room.getFen();
        String code = room.getCode();

        List<Piece> pieces = MoveUtils.handleMove(FenUtils.parseFenToPieces(fen), move);
        pieces = updateMovedPieceInList(pieces, movedPiece, targetPosition);

        String nextTurn = getNextTurn(fen, move);
        String newFen = FenUtils.piecesToFen(pieces, nextTurn);

        boolean checkMate = isCheckMate(move);

        if (targetPiece != null) {
            String capturedPieces = CapturedPieceUtil.recordCapture(room.getCapturedPieces(), targetPiece);
            room.setCapturedPieces(capturedPieces);
        }
        room.setFen(newFen);
        room.setLastActivity(LocalDateTime.now());

        PieceMovedResponseDTO responseDTO = new PieceMovedResponseDTO().setMove(move).setFen(newFen);
        this.broadcastRoomUpdate(code, responseDTO);

        if (checkMate) {
            GameStatus whoWon = Objects.equals(move.getMoveDetails().getTargetPiece().getColor(), "w") ?
                    GameStatus.BLACK_WON :
                    GameStatus.WHITE_WON;
            room.setRoomStatus(RoomStatus.OCCUPIED);
            room.setGameStatus(whoWon);

            AuthenticatedUserDTO whitePlayerDTO = null, blackPlayerDTO = null;

            User whitePlayer = this.userService.getUserById(room.getWhitePlayer());
            User blackPlayer = this.userService.getUserById(room.getBlackPlayer());
            if (room.getWhitePlayer() != null) {
                whitePlayerDTO = this.getAuthenticatedUserDto(whitePlayer);
            }
            if (room.getBlackPlayer() != null) {
                blackPlayerDTO = this.getAuthenticatedUserDto(blackPlayer);
            }
            RoomDTO roomDTO = this.getRoomDto(room, whitePlayerDTO, blackPlayerDTO);

            room.setBlackPlayer(null).setWhitePlayer(null);
            whitePlayer.setInGame(false);
            blackPlayer.setInGame(false);

            this.userService.updateUser(whitePlayer);
            this.userService.updateUser(blackPlayer);
            this.broadcastRoomUpdate(code, roomDTO);
            this.broadcastRoomUpdate(code, "Game has ended.");
        }
        this.roomRepository.save(room);
    }

    public Room getRoomByUserId(UUID userId) {
        return this.roomRepository.findRoomByUserId(userId).orElse(null);
    }

    public void updateRoom(Room room) {
        this.roomRepository.save(room);
    }

    public void handlePlayerJoinRoom(String code, User user) {
        Room room = this.roomRepository.findByCode(code).orElseThrow(() -> new RoomNotFoundException(code));

        String message = "Player " + user.getUsername() + " joined the room.";
        this.broadcastRoomUpdate(code, message);

        user.setInGame(true);
        this.userService.updateUser(user);

        if (
                room.getWhitePlayer() != null &&
                room.getBlackPlayer() != null &&
                room.getGameStatus() != GameStatus.IN_PROGRESS
        ) {
            room.setGameStatus(GameStatus.IN_PROGRESS);
            this.roomRepository.save(room);
            this.broadcastRoomUpdate(room.getCode(), "Game resumed.");
        }

        AuthenticatedUserDTO whitePlayerDTO = null, blackPlayerDTO = null;

        if (room.getWhitePlayer() != null) {
            whitePlayerDTO = this.getAuthenticatedUserDto(room.getWhitePlayer());
        }
        if (room.getBlackPlayer() != null) {
            blackPlayerDTO = this.getAuthenticatedUserDto(room.getBlackPlayer());
        }

        RoomDTO roomDTO = this.getRoomDto(room, whitePlayerDTO, blackPlayerDTO);
        this.broadcastRoomUpdate(code, roomDTO);
    }

    public RoomDTO handlePlayerLeaveRoom(String code, User user) {
        Room room = this.roomRepository.findByCode(code)
                .orElseThrow(() -> new RoomNotFoundException(code));

        String message = "Player " + user.getUsername() + " left the room.";
        this.broadcastRoomUpdate(code, message);

        user.setInGame(false);
        this.userService.updateUser(user);

        if (room.getGameStatus() == GameStatus.IN_PROGRESS) {
            room.setGameStatus(GameStatus.PAUSED);
            this.roomRepository.save(room);
            this.broadcastRoomUpdate(room.getCode(), "Game paused.");
        }

        AuthenticatedUserDTO whitePlayerDTO = null, blackPlayerDTO = null;

        if (room.getWhitePlayer() != null) {
            whitePlayerDTO = this.getAuthenticatedUserDto(room.getWhitePlayer());
        }

        if (room.getBlackPlayer() != null) {
            blackPlayerDTO = this.getAuthenticatedUserDto(room.getBlackPlayer());
        }

        RoomDTO roomDTO = this.getRoomDto(room, whitePlayerDTO, blackPlayerDTO);
        this.broadcastRoomUpdate(code, roomDTO);
        return roomDTO;
    }

    private void broadcastRoomUpdate(String roomCode, Object data) {
        this.simpMessagingTemplate.convertAndSend("/topic/room." + roomCode, data);
    }

    private AuthenticatedUserDTO getAuthenticatedUserDto(UUID id) {
        User user = this.userService.getUserById(id);
        return this.getAuthenticatedUserDto(user);
    }
    private AuthenticatedUserDTO getAuthenticatedUserDto(User user) {
        return new AuthenticatedUserDTO().setEmail(user.getEmail())
                .setUsername(user.getUsername())
                .setInGame(user.getInGame());
    }

    private RoomDTO getRoomDto(Room room, AuthenticatedUserDTO whitePlayerDTO, AuthenticatedUserDTO blackPlayerDTO) {
        return new RoomDTO()
                .setCode(room.getCode())
                .setCapturedPieces(room.getCapturedPieces())
                .setFen(room.getFen())
                .setBlackPlayer(blackPlayerDTO)
                .setWhitePlayer(whitePlayerDTO)
                .setId(room.getId())
                .setLastActivity(room.getLastActivity())
                .setRoomStatus(room.getRoomStatus())
                .setGameStatus(room.getGameStatus());
    }

    // TODO: Find a better way to generate Unique RoomID
    private String generateUniqueRoomId() {
        String code;
        Optional<Room> existingRoom;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < RoomConstants.ROOM_CODE_LENGTH; i++) {
                sb.append(RoomConstants.CHARACTERS.charAt(random.nextInt(RoomConstants.CHARACTERS.length())));
            }
            code = sb.toString();
            existingRoom = roomRepository.findByCode(code);
        } while (existingRoom.isPresent());

        return code;
    }
}
