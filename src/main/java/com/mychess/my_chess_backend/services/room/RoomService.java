package com.mychess.my_chess_backend.services.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychess.my_chess_backend.dtos.responses.auth.AuthenticatedUserDTO;
import com.mychess.my_chess_backend.dtos.responses.room.PieceMovedResponseDTO;
import com.mychess.my_chess_backend.dtos.responses.room.RoomDTO;
import com.mychess.my_chess_backend.dtos.shared.Piece;
import com.mychess.my_chess_backend.dtos.shared.Move;
import com.mychess.my_chess_backend.dtos.shared.Position;
import com.mychess.my_chess_backend.models.Room;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.repositories.RoomRepository;
import com.mychess.my_chess_backend.services.user.UserService;
import com.mychess.my_chess_backend.utils.CapturedPieceUtil;
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
public class RoomService extends RoomServiceHelper {
    private final RoomRepository roomRepository;
    private final ExecutorService executorService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String DEFAULT_CAPTURED_PIECES = "r0n0b0q0p0k0/R0N0B0Q0P0K0";
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
        Optional<Room> existingRoom = this.roomRepository.findRoomByUserId(whitePlayer.getId());
        Room room = existingRoom.orElseGet(() -> Room.builder()
                .code(this.generateUniqueRoomId())
                .fen(FenUtils.DEFAULT_CHESSBOARD_FEN)
                .capturedPieces(DEFAULT_CAPTURED_PIECES)
                .whitePlayer(whitePlayer.getId())
                .roomStatus(RoomStatus.AVAILABLE)
                .gameStatus(GameStatus.WAITING)
                .lastActivity(LocalDateTime.now())
                .build());

        whitePlayer.setInGame(true);
        if (existingRoom.isEmpty()) {
            this.roomRepository.save(room);
        }
        whitePlayer.setInGame(true);
        this.userService.updateUser(whitePlayer);
        return new RoomDTO().setCode(room.getCode());
    }

    public RoomDTO joinRoom(User blackPlayer, String code) {
        Room existingRoom = this.roomRepository.findRoomByUserId(blackPlayer.getId()).orElse(null);
        Room room = this.roomRepository.findByCode(code).orElse(null);
        if (room != null) {
            if (room.getWhitePlayer().equals(blackPlayer.getId())) {
                return null;
            }
            if (existingRoom != null && !existingRoom.getCode().equals(room.getCode())) {
                 return null;
            }

            UUID existingBlackPlayer = room.getBlackPlayer();
            UUID whitePlayer = room.getWhitePlayer();
            if (
                    existingBlackPlayer != null &&
                    (!existingBlackPlayer.equals(blackPlayer.getId()) || !whitePlayer.equals(blackPlayer.getId()))
            ) {
                return null;
            }

            room.setBlackPlayer(blackPlayer.getId())
                    .setRoomStatus(RoomStatus.OCCUPIED)
                    .setGameStatus(GameStatus.IN_PROGRESS);

            blackPlayer.setInGame(true);
            this.userService.updateUser(blackPlayer);

            RoomDTO roomDto = new RoomDTO().setCode(room.getCode());
            // TODO: Add proper error handling
            String roomDtoJson = "";
            try {
                roomDtoJson = this.objectMapper.writeValueAsString(roomDto);
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.updateRoom(room, "Opponent joined !");
            this.broadcastRoomUpdate(room.getCode(), roomDtoJson);
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
                    .setCapturedPieces(room.getCapturedPieces())
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
        Piece movedPiece = move.getPiece();
        Piece targetPiece = move.getMoveDetails().getTargetPiece();
        Position targetPosition = move.getTo();

        Room room = this.roomRepository.findByCode(code).orElseThrow(() -> new Exception("The room does not exist"));

        String fen = room.getFen();
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
        this.broadcastRoomUpdate(code, this.objectMapper.writeValueAsString(responseDTO));

        if (checkMate) {
            GameStatus whoWon = Objects.equals(move.getMoveDetails().getTargetPiece().getColor(), "w") ?
                    GameStatus.BLACK_WON :
                    GameStatus.WHITE_WON;
            room.setRoomStatus(RoomStatus.OCCUPIED);
            room.setGameStatus(whoWon);

            User whitePlayer = null, blackPlayer = null;
            AuthenticatedUserDTO whitePlayerDTO = null, blackPlayerDTO = null;

            if (room.getWhitePlayer() != null) {
                whitePlayer = this.userService.getUserById(room.getWhitePlayer());
            }
            if (room.getBlackPlayer() != null) {
                blackPlayer = this.userService.getUserById(room.getBlackPlayer());
            }
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
            RoomDTO roomDTO = new RoomDTO()
                    .setId(room.getId())
                    .setRoomStatus(room.getRoomStatus())
                    .setGameStatus(room.getGameStatus())
                    .setCode(room.getCode())
                    .setFen(newFen)
                    .setCapturedPieces(room.getCapturedPieces())
                    .setLastActivity(room.getLastActivity())
                    .setBlackPlayer(blackPlayerDTO)
                    .setWhitePlayer(whitePlayerDTO);
            this.broadcastRoomUpdate(code, this.objectMapper.writeValueAsString(roomDTO));
            this.completeBroadcastingRoomUpdates(room.getCode());
            room.setBlackPlayer(null).setWhitePlayer(null);
            this.roomRepository.save(room);
        } else {
            this.roomRepository.save(room);
        }
    }

    public SseEmitter subscribeToRoomUpdates(String code) {
        SseEmitter emitter = new SseEmitter((long) Integer.MAX_VALUE);
        System.out.println("------- New person subscribed -------" + this.roomSubscribers);
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
                System.out.println("------- Failed to send update, completing emitter with error -------");
                emitter.completeWithError(e);
                // The onError callback will handle cleanup
            } catch (IllegalStateException e) {
                // Emitter already completed, ignore
                System.out.println("------- Attempted to send to already completed emitter -------");
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
