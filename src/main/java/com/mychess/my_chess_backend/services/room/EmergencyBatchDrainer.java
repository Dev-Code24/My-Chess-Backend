package com.mychess.my_chess_backend.services.room;

import com.mychess.my_chess_backend.models.Room;
import com.mychess.my_chess_backend.repositories.RoomRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmergencyBatchDrainer {
  private final EmergencyBufferService emergencyBuffer;
  private final RoomRepository roomRepository;

  public EmergencyBatchDrainer(EmergencyBufferService emergencyBuffer, RoomRepository roomRepository) {
    this.emergencyBuffer = emergencyBuffer;
    this.roomRepository = roomRepository;
  }

  // Runs frequently to check if there are buffered moves to "trickle" into the DB
  @Scheduled(fixedDelay = 2000)
  @Transactional
  public void drainEmergencyQueue() {
    if (emergencyBuffer.getEmergencyQueue().isEmpty()) { return; }

    List<EmergencyBufferService.QueueItem> batch = new ArrayList<>();
    emergencyBuffer.getEmergencyQueue().drainTo(batch, 50);

    Map<String, EmergencyBufferService.QueueItem> latestStates = batch.stream()
        .collect(Collectors.toMap(
            EmergencyBufferService.QueueItem::code,
            item -> item,
            (existing, replacement) -> replacement
        ));

    List<Room> rooms = roomRepository.findAllByCodeIn(latestStates.keySet());

    rooms.forEach(room -> {
      var latest = latestStates.get(room.getCode()).cache();
      room.setFen(latest.getFen());
      room.setCapturedPieces(latest.getCapturedPieces());
      room.setGameStatus(latest.getGameStatus());
      room.setLastActivity(latest.getLastActivity());
    });

    roomRepository.saveAll(rooms);
  }
}