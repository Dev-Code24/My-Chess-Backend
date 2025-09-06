package com.mychess.my_chess_backend.services.room;

import com.mychess.my_chess_backend.models.Room;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.repositories.RoomRepository;
import com.mychess.my_chess_backend.utils.enums.GameStatus;
import com.mychess.my_chess_backend.utils.enums.RoomStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 6;
    private static final Random random = new Random();

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Room createRoom(User whitePlayer) {
        Room newRoom = Room.builder()
                .code(this.generateUniqueRoomId())
                .whitePlayer(whitePlayer.getId())
                .roomStatus(RoomStatus.AVAILABLE)
                .gameStatus(GameStatus.WAITING)
                .lastActivity(LocalDateTime.now())
                .build();
        this.roomRepository.save(newRoom);
        return newRoom;
    }

    public Room joinRoom(User blackPlayer, String roomId) {
        Optional<Room> room = this.roomRepository.findByCode(roomId);
        if (room.isPresent()) {
            Room currentRoom = room.get();

            if (currentRoom.getWhitePlayer().equals(blackPlayer.getId())) { return null; }

            currentRoom.setBlackPlayer(blackPlayer.getId())
                    .setRoomStatus(RoomStatus.OCCUPIED)
                    .setGameStatus(GameStatus.IN_PROGRESS);
            this.roomRepository.save(currentRoom);

            return currentRoom;
        }

        return null;
    }
    // TODO: Find a better way to generate Unique RoomID
    private String generateUniqueRoomId() {
        String code;
        Optional<Room> existingRoom;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            code = sb.toString();
            existingRoom = roomRepository.findByCode(code);
        } while (existingRoom.isPresent());

        return code;
    }
}
