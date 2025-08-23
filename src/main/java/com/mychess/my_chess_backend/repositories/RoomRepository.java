package com.mychess.my_chess_backend.repositories;

import com.mychess.my_chess_backend.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface RoomRepository extends JpaRepository<Room, UUID> { }
