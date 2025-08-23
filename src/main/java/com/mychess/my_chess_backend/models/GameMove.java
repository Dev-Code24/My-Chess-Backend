package com.mychess.my_chess_backend.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "moves")
public class GameMove {
    @Id
    @UuidGenerator
    private UUID id;
    @ManyToOne
    private Room room;
    @ManyToOne
    private User movedBy;

    private int moveNumber;
    private String moveNotation;
}
