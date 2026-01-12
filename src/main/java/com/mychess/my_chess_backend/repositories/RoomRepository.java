package com.mychess.my_chess_backend.repositories;

import com.mychess.my_chess_backend.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByCode(String code);
    List<Room> findAllByCodeIn(Collection<String> codes);

    @Query("SELECT room FROM Room room WHERE :userId IN (room.blackPlayer, room.whitePlayer)")
    Optional<Room> findRoomByUserId(@Param("userId") UUID userId);

}
