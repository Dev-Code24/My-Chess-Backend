package com.mychess.my_chess_backend.services.room;

import com.mychess.my_chess_backend.dtos.redis.MoveCache;
import lombok.Getter;
import org.springframework.stereotype.Service;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class EmergencyBufferService {
  public record QueueItem(String code, MoveCache cache) {}

  @Getter
  public final LinkedBlockingQueue<QueueItem> emergencyQueue = new LinkedBlockingQueue<>(100000);

  public void bufferMove(String code, MoveCache cache) {
    emergencyQueue.offer(new QueueItem(code, cache));
  }
}