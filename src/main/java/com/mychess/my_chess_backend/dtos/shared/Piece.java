package com.mychess.my_chess_backend.dtos.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Piece {
    private String id;
    private byte col;
    private byte row;
    private String color;
    private String type;
    private Boolean enPassantAvailable;
    private boolean hasMoved;
}
