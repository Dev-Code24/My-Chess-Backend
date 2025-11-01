package com.mychess.my_chess_backend.dtos.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class PieceDetails {
    private String id;
    private String type;
    private String color;
    private int row;
    private int col;
    private Boolean hasMoved;
    private Boolean enPassantAvailable;
}
