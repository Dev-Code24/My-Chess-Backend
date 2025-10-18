package com.mychess.my_chess_backend.services.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychess.my_chess_backend.dtos.responses.auth.AuthenticatedUserDTO;
import com.mychess.my_chess_backend.dtos.responses.room.PieceMovedResponseDTO;
import com.mychess.my_chess_backend.dtos.responses.room.RoomDTO;
import com.mychess.my_chess_backend.dtos.shared.MoveDetails;
import com.mychess.my_chess_backend.dtos.shared.Piece;
import com.mychess.my_chess_backend.dtos.shared.Move;
import com.mychess.my_chess_backend.dtos.shared.Position;
import com.mychess.my_chess_backend.models.Room;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.repositories.RoomRepository;
import com.mychess.my_chess_backend.services.user.UserService;
import com.mychess.my_chess_backend.utils.FenUtils;
import com.mychess.my_chess_backend.utils.MoveUtils;
import com.mychess.my_chess_backend.utils.enums.GameStatus;
import com.mychess.my_chess_backend.utils.enums.RoomStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final ExecutorService executorService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String DEFAULT_CHESSBOARD_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final Random random = new Random();
    private final Map<String, List<SseEmitter>> roomSubscribers = new ConcurrentHashMap<>();

    public RoomService(
            RoomRepository roomRepository,
            ExecutorService executorService,
            UserService userService,
            ObjectMapper objectMapper
    ) {
        this.roomRepository = roomRepository;
        this.executorService = executorService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public RoomDTO createRoom(User whitePlayer) {
        Room newRoom = Room.builder()
                .code(this.generateUniqueRoomId())
                .fen(DEFAULT_CHESSBOARD_FEN)
                .whitePlayer(whitePlayer.getId())
                .roomStatus(RoomStatus.AVAILABLE)
                .gameStatus(GameStatus.WAITING)
                .lastActivity(LocalDateTime.now())
                .build();

        whitePlayer.setInGame(true);
        this.roomRepository.save(newRoom);
        this.userService.updateUser(whitePlayer);
        return new RoomDTO().setCode(newRoom.getCode());
    }

    public RoomDTO joinRoom(User blackPlayer, String code) {
        Optional<Room> room = this.roomRepository.findByCode(code);
        if (room.isPresent()) {
            Room currentRoom = room.get();

            if (currentRoom.getWhitePlayer().equals(blackPlayer.getId())) { return null; }

            UUID existingBlackPlayer = currentRoom.getBlackPlayer();
            if (
                    existingBlackPlayer != null &&
                    !existingBlackPlayer.equals(blackPlayer.getId())
            ) { return null; }

            currentRoom.setBlackPlayer(blackPlayer.getId())
                    .setRoomStatus(RoomStatus.OCCUPIED)
                    .setGameStatus(GameStatus.IN_PROGRESS);

            blackPlayer.setInGame(true);
            User whitePlayer = this.userService.getUserById(currentRoom.getWhitePlayer());
            whitePlayer.setInGame(true);

            this.userService.updateUser(blackPlayer);
            this.userService.updateUser(whitePlayer);

            RoomDTO roomDto = new RoomDTO()
                    .setCode(currentRoom.getCode())
                    .setFen(currentRoom.getFen())
                    .setId(currentRoom.getId())
                    .setRoomStatus(currentRoom.getRoomStatus())
                    .setGameStatus(currentRoom.getGameStatus())
                    .setLastActivity(currentRoom.getLastActivity())
                    .setWhitePlayer(
                            new AuthenticatedUserDTO(
                                    whitePlayer.getEmail(),
                                    whitePlayer.getUsername(),
                                    whitePlayer.getInGame()
                            )
                    )
                    .setBlackPlayer(
                            new AuthenticatedUserDTO(
                                    blackPlayer.getEmail(),
                                    blackPlayer.getUsername(),
                                    blackPlayer.getInGame()
                            )
                    );
            // TODO: Add proper error handling
            String roomDtoJson = "";
            try {
                roomDtoJson = this.objectMapper.writeValueAsString(roomDto);
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.updateRoom(currentRoom, "Opponent joined !");
            this.broadcastRoomUpdate(currentRoom.getCode(), roomDtoJson);
            return roomDto;
        }
        return null;
    }

    public RoomDTO getRoom(String code) {
        Optional<Room> optionalRoom = this.roomRepository.findByCode(code);
        if (optionalRoom.isPresent()) {
            Room room = optionalRoom.get();
            User whitePlayer = null, blackPlayer = null;
            if (room.getWhitePlayer() != null) {
                whitePlayer = this.userService.getUserById(room.getWhitePlayer());
            }
            if (room.getBlackPlayer() != null) {
                blackPlayer = this.userService.getUserById(room.getBlackPlayer());
            }

            AuthenticatedUserDTO whitePlayerDTO = null, blackPlayerDTO = null;
            if (whitePlayer != null) {
                whitePlayerDTO = new AuthenticatedUserDTO()
                        .setEmail(whitePlayer.getEmail())
                        .setUsername(whitePlayer.getUsername())
                        .setInGame(whitePlayer.getInGame());
            }
            if (blackPlayer != null) {
                blackPlayerDTO = new AuthenticatedUserDTO()
                        .setEmail(blackPlayer.getEmail())
                        .setUsername(blackPlayer.getUsername())
                        .setInGame(blackPlayer.getInGame());
            }
            return new RoomDTO()
                    .setId(room.getId())
                    .setCode(room.getCode())
                    .setFen(room.getFen())
                    .setRoomStatus(room.getRoomStatus())
                    .setGameStatus(room.getGameStatus())
                    .setLastActivity(room.getLastActivity())
                    .setWhitePlayer(whitePlayerDTO)
                    .setBlackPlayer(blackPlayerDTO);
        }
        return null;
    }

    public void updateRoom(Room room, Object data) {
        this.roomRepository.save(room);
        this.broadcastRoomUpdate(room.getCode(), data);
    }

    public void move(Move move, String code) throws Exception {
        Room room = this.roomRepository.findByCode(code).orElseThrow(() -> new Exception("Sorry the room does not exist"));
        Piece movedPiece = move.getPiece();
        Position targetPosition = move.getTo();
        List<Piece> pieces = MoveUtils.handleMove(FenUtils.parseFenToPieces(room.getFen()), move);

        for (Piece piece : pieces) {
            if (piece.getId().equals(movedPiece.getId())) {
                byte finalTargetRow = targetPosition.getRow();
                if (piece.getColor().equals("w")) {
                    finalTargetRow = (byte) (7 - targetPosition.getRow());
                }
                piece.setRow(finalTargetRow);
                piece.setCol(targetPosition.getCol());
                piece.setHasMoved(true);
                break;
            }
        }

        String newFen = FenUtils.piecesToFen(pieces, FenUtils.getNextTurn(room.getFen()));
        room.setFen(newFen);
        room.setLastActivity(LocalDateTime.now());

        PieceMovedResponseDTO responseDTO = new PieceMovedResponseDTO()
                .setMoveDetails(move)
                .setFen(newFen);

        this.broadcastRoomUpdate(code, this.objectMapper.writeValueAsString(responseDTO));
        this.roomRepository.save(room);
    }

    public SseEmitter subscribeToRoomUpdates(String code) {
        SseEmitter emitter = new SseEmitter((long) Integer.MAX_VALUE);
        System.out.println("------- New person subscribed -------");
        this.roomSubscribers.computeIfAbsent(code, (_) -> new ArrayList<>()).add(emitter);
        emitter.onCompletion(() -> this.removeRoomSubscriber(code, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            this.removeRoomSubscriber(code, emitter);
        });
        emitter.onError((e) -> this.removeRoomSubscriber(code, emitter));
        this.sendUpdateToSubscriber(emitter, "Connected for live updates");
        return emitter;
    }

    public void completeBroadcastingRoomUpdates(String roomCode) {
        List<SseEmitter> emitters = this.roomSubscribers.get(roomCode);
        this.broadcastRoomUpdate(roomCode, "Game has ended.");
        if (emitters != null) {
            for (var emitter : emitters) {
                emitter.complete();
            }
            this.roomSubscribers.remove(roomCode);
        }
    }

    private void broadcastRoomUpdate(String roomCode, Object data) {
        List<SseEmitter> emitters = this.roomSubscribers.get(roomCode);
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                this.sendUpdateToSubscriber(emitter, data);
            }
        }
    }

    private void sendUpdateToSubscriber(SseEmitter emitter, Object data) {
        this.executorService.execute(() -> {
            try {
                emitter.send(SseEmitter.event().data(data.toString() + "\n\n"));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });
    }

    private void removeRoomSubscriber(String roomCode, SseEmitter emitter) {
        List<SseEmitter> emitters = this.roomSubscribers.get(roomCode);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }

    // TODO: Find a better way to generate Unique RoomID
    private String generateUniqueRoomId() {
        String code;
        Optional<Room> existingRoom;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
                sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            code = sb.toString();
            existingRoom = roomRepository.findByCode(code);
        } while (existingRoom.isPresent());

        return code;
    }
}
