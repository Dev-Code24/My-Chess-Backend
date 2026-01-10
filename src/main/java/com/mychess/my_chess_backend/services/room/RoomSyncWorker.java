package com.mychess.my_chess_backend.services.room;

import com.mychess.my_chess_backend.dtos.redis.MoveCache;
import com.mychess.my_chess_backend.models.Room;
import com.mychess.my_chess_backend.repositories.RoomRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomSyncWorker {
  private final RedisGameService redisGameService;
  private final RoomRepository roomRepository;
  private final StringRedisTemplate stringRedisTemplate;

  private static final String SYNC_SET_KEY = "rooms_to_sync";

  public RoomSyncWorker(
      RedisGameService redisGameService,
      RoomRepository roomRepository,
      StringRedisTemplate stringRedisTemplate
  ) {
    this.redisGameService = redisGameService;
    this.roomRepository = roomRepository;
    this.stringRedisTemplate = stringRedisTemplate;
  }

  @Scheduled(fixedDelay = 10000)
  @Transactional
  public void syncRedisToDb() {
    Set<String> roomCodes = stringRedisTemplate.opsForSet().members(SYNC_SET_KEY);
    if (roomCodes == null || roomCodes.isEmpty()) { return; }

    List<Room> rooms = roomRepository.findAllByCodeIn(roomCodes);

    List<Room> updatedRooms = rooms.stream().map(room -> {
      MoveCache cache = redisGameService.getMoveCache(room.getCode());
      if (cache != null) {
        room.setFen(cache.getFen());
        room.setCapturedPieces(cache.getCapturedPieces());
        room.setGameStatus(cache.getGameStatus());
        room.setLastActivity(cache.getLastActivity());
        return room;
      }

      return null;
    }).filter(Objects::nonNull).collect(Collectors.toList());

    if (!updatedRooms.isEmpty()) {
      roomRepository.saveAll(updatedRooms);
    }

    stringRedisTemplate.delete(SYNC_SET_KEY);
  }
}