package com.mychess.my_chess_backend.models;

import com.mychess.my_chess_backend.dtos.shared.PieceDetails;
import com.mychess.my_chess_backend.utils.enums.GameStatus;
import com.mychess.my_chess_backend.utils.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoomStatus roomStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GameStatus gameStatus;

    @Column(nullable = false, length = 6)
    private String code;
    @Column(nullable = false, length = 100)
    private String fen;
    @Column(nullable = false)
    private String capturedPieces;

    private UUID whitePlayer;
    private UUID blackPlayer;

    private LocalDateTime lastActivity;
}
