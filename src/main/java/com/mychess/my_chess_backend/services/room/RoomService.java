package com.mychess.my_chess_backend.services.room;

import com.mychess.my_chess_backend.dtos.redis.MoveCache;
import com.mychess.my_chess_backend.dtos.responses.auth.AuthenticatedUserDTO;
import com.mychess.my_chess_backend.dtos.responses.room.PieceMovedResponseDTO;
import com.mychess.my_chess_backend.dtos.responses.room.RoomDTO;
import com.mychess.my_chess_backend.dtos.shared.Piece;
import com.mychess.my_chess_backend.dtos.shared.Move;
import com.mychess.my_chess_backend.dtos.shared.Position;
import com.mychess.my_chess_backend.exceptions.room.RoomErrorMessage;
import com.mychess.my_chess_backend.exceptions.room.MoveNotAllowed;
import com.mychess.my_chess_backend.exceptions.room.StaleMoveException;
import com.mychess.my_chess_backend.models.Room;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.repositories.RoomRepository;
import com.mychess.my_chess_backend.services.events.RedisEventPublisher;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RoomService extends RoomServiceHelper {
    private final RoomRepository roomRepository;
    private final UserService userService;
    private final RedisGameService redisGameService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RedisEventPublisher redisEventPublisher;

    private static final Random random = new Random();

    public RoomService(
        RoomRepository roomRepository,
        UserService userService,
        RedisGameService redisGameService,
        SimpMessagingTemplate simpMessagingTemplate,
        RedisEventPublisher redisEventPublisher
    ) {
        this.roomRepository = roomRepository;
        this.userService = userService;
        this.redisGameService = redisGameService;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.redisEventPublisher = redisEventPublisher;
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
            throw new RoomJoinNotAllowedException(RoomErrorMessage.ALREADY_IN_ROOM.getValue());
        }

        if (room.getWhitePlayer().equals(blackPlayer.getId())) {
            throw new RoomJoinNotAllowedException(RoomErrorMessage.CANNOT_JOIN_YOUR_OWN_ROOM.getValue());
        }

        if (room.getBlackPlayer() != null && !room.getBlackPlayer().equals(blackPlayer.getId())) {
            throw new RoomJoinNotAllowedException(RoomErrorMessage.ROOM_ALREADY_FULL.getValue());
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

        // Check Redis cache for latest game state
        MoveCache cache = this.redisGameService.getMoveCache(code);

        // Use cache values if available, otherwise fall back to database
        String fen = room.getFen();
        String capturedPieces = room.getCapturedPieces();
        Long moveSequence = room.getMoveSequence();

        if (cache != null) {
            fen = cache.getFen();
            capturedPieces = cache.getCapturedPieces();
            moveSequence = cache.getMoveSequence() != null ? cache.getMoveSequence() : 0L;
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
            .setCapturedPieces(capturedPieces)
            .setFen(fen)
            .setRoomStatus(room.getRoomStatus())
            .setGameStatus(room.getGameStatus())
            .setLastActivity(room.getLastActivity())
            .setMoveSequence(moveSequence)
            .setWhitePlayer(whitePlayerDTO)
            .setBlackPlayer(blackPlayerDTO);
    }

    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void processPlayerMove(
        Move move,
        String roomId,
        User player
    ) {
        MoveCache cache = this.redisGameService.getMoveCache(roomId);

        if (cache == null) {
            Room room = this.roomRepository.findByCode(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
            cache = MoveCache.builder()
                .fen(room.getFen())
                .capturedPieces(room.getCapturedPieces())
                .whitePlayerId(room.getWhitePlayer())
                .blackPlayerId(room.getBlackPlayer())
                .gameStatus(room.getGameStatus())
                .lastActivity(room.getLastActivity())
                .moveSequence(room.getMoveSequence() != null ? room.getMoveSequence() : 0L)
                .build();
            redisGameService.saveMoveCache(roomId, cache);
        }

        // Idempotency check: Ensure move sequence is valid
        if (move.getExpectedMoveSequence() != null) {
            Long currentSequence = cache.getMoveSequence() != null ? cache.getMoveSequence() : 0L;
            if (!move.getExpectedMoveSequence().equals(currentSequence)) {
                throw new StaleMoveException(move.getExpectedMoveSequence(), currentSequence);
            }
        }

        if (!Objects.equals(cache.getWhitePlayerId(), player.getId()) &&
            !Objects.equals(cache.getBlackPlayerId(), player.getId())) {
            throw new RoomJoinNotAllowedException(RoomErrorMessage.UNAUTHORIZED_MOVE.getValue());
        }

        if (cache.getGameStatus() != GameStatus.IN_PROGRESS) {
            throw new RoomJoinNotAllowedException(RoomErrorMessage.GAME_INACTIVE.getValue());
        }

        boolean isWhiteTurn = FenUtils.getTurn(cache.getFen()).equals("w");
        boolean isPlayerWhite = Objects.equals(cache.getWhitePlayerId(), player.getId());

        if (isWhiteTurn && !isPlayerWhite && move.getMoveDetails().getPromotion() != Boolean.TRUE) {
            throw new MoveNotAllowed(RoomErrorMessage.WHITES_TURN.getValue());
        }

        if (!isWhiteTurn && isPlayerWhite && move.getMoveDetails().getPromotion() != Boolean.TRUE) {
            throw new MoveNotAllowed(RoomErrorMessage.BLACKS_TURN.getValue());
        }

        this.handleMove(move, roomId, cache);
    }

    public void handleMove(Move move, String roomId, MoveCache cache) {
        Piece movedPiece = move.getPiece();
        Piece targetPiece = move.getMoveDetails().getTargetPiece();
        Position targetPosition = move.getTo();
        String fen = cache.getFen();

        List<Piece> pieces = MoveUtils.handleMove(FenUtils.parseFenToPieces(fen), move);
        pieces = updateMovedPieceInList(pieces, movedPiece, targetPosition);

        String nextTurn = getNextTurn(fen, move);
        String newFen = FenUtils.piecesToFen(pieces, nextTurn);

        boolean checkMate = isCheckMate(move);

        if (targetPiece != null) {
            String capturedPieces = CapturedPieceUtil.recordCapture(cache.getCapturedPieces(), targetPiece);
            cache.setCapturedPieces(capturedPieces);
        }
        cache.setFen(newFen);
        cache.setLastActivity(LocalDateTime.now());

        Long currentSequence = cache.getMoveSequence() != null ? cache.getMoveSequence() : 0L;
        cache.setMoveSequence(currentSequence + 1);

        PieceMovedResponseDTO responseDTO = new PieceMovedResponseDTO()
            .setMove(move)
            .setFen(newFen)
            .setMoveSequence(cache.getMoveSequence());

        // Broadcast to local WebSocket subscribers
        this.broadcastRoomUpdate(roomId, responseDTO);

        // Publish to Redis Pub/Sub for other server instances
        redisEventPublisher.publishMoveEvent(roomId, responseDTO);

        if (checkMate) {
            Room room = this.roomRepository.findByCode(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
            room.setFen(cache.getFen());
            room.setMoveSequence(cache.getMoveSequence());

            GameStatus whoWon = Objects.equals(move.getMoveDetails().getTargetPiece().getColor(), "w") ?
                GameStatus.BLACK_WON :
                GameStatus.WHITE_WON;
            room.setRoomStatus(RoomStatus.OCCUPIED);
            room.setGameStatus(whoWon);

            AuthenticatedUserDTO whitePlayerDTO = null, blackPlayerDTO = null;

            User whitePlayer = this.userService.getUserById(room.getWhitePlayer());
            User blackPlayer = this.userService.getUserById(room.getBlackPlayer());
            if (cache.getWhitePlayerId() != null) {
                whitePlayerDTO = this.getAuthenticatedUserDto(whitePlayer);
            }
            if (cache.getBlackPlayerId() != null) {
                blackPlayerDTO = this.getAuthenticatedUserDto(blackPlayer);
            }
            RoomDTO roomDTO = this.getRoomDto(room, whitePlayerDTO, blackPlayerDTO);

            cache.setBlackPlayerId(null).setWhitePlayerId(null);
            whitePlayer.setInGame(false);
            blackPlayer.setInGame(false);

            this.userService.updateUser(whitePlayer);
            this.userService.updateUser(blackPlayer);
            this.broadcastRoomUpdate(roomId, roomDTO);
            this.broadcastRoomUpdate(roomId, "Game has ended.");
            this.roomRepository.save(room);
        } else {
            this.redisGameService.saveMoveCache(roomId, cache);
        }
    }

    public Room getRoomByUserId(UUID userId) {
        return this.roomRepository.findRoomByUserId(userId).orElse(null);
    }

    public void updateRoom(Room room) {
        this.roomRepository.save(room);
    }

    @Retryable(
        retryFor = { ObjectOptimisticLockingFailureException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
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

    @Retryable(
        retryFor = { ObjectOptimisticLockingFailureException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
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
