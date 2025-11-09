package com.mychess.my_chess_backend.controllers.room;

import com.mychess.my_chess_backend.dtos.requests.room.JoiningRoomDTO;
import com.mychess.my_chess_backend.dtos.responses.BasicResponseDTO;
import com.mychess.my_chess_backend.dtos.responses.room.RoomDTO;
import com.mychess.my_chess_backend.dtos.shared.Move;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.services.room.RoomService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequestMapping("/room")
@RestController
public class RoomController {
    private final RoomService roomService;

    public RoomController(
            RoomService roomService
    ) {
        this.roomService = roomService;
    }

    @PostMapping("/create")
    public ResponseEntity<BasicResponseDTO<RoomDTO>> createRoom(HttpServletRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User whitePlayer = (User) authentication.getPrincipal();
        RoomDTO newRoom = this.roomService.createRoom(whitePlayer);

        return ResponseEntity.ok(new BasicResponseDTO<>(
                "success",
                HttpStatus.OK.value(),
                newRoom,
                req.getRequestURI()
        ));
    }

    @PostMapping("/join")
    public ResponseEntity<BasicResponseDTO<RoomDTO>> joinRoom(
            HttpServletRequest req,
            @RequestBody JoiningRoomDTO roomDetails
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User blackPlayer = (User) authentication.getPrincipal();
        RoomDTO room = this.roomService.joinRoom(blackPlayer, roomDetails.getCode());

        return ResponseEntity.ok(new BasicResponseDTO<>(
                "success",
                HttpStatus.OK.value(),
                room,
                req.getRequestURI()
        ));
    }

    @PostMapping("/move/{code}")
    public ResponseEntity<BasicResponseDTO<String>> pieceMoved(
            @PathVariable String code,
            @RequestBody Move move,
            HttpServletRequest req
    ) {
        this.roomService.move(move, code);
        return ResponseEntity.ok(new BasicResponseDTO<>(
                "success",
                HttpStatus.OK.value(),
                "Updated Piece",
                req.getRequestURI()
        ));
    }

    @GetMapping("/{code}")
    public ResponseEntity<BasicResponseDTO<RoomDTO>> roomDetails(
            @PathVariable String code,
            HttpServletRequest req
    ) {
        RoomDTO room = this.roomService.getRoom(code);

        return ResponseEntity.ok(new BasicResponseDTO<>(
                "success",
                HttpStatus.OK.value(),
                room,
                req.getRequestURI()
        ));
    }

    @GetMapping("/live/{code}")
    public SseEmitter streamEvents(@PathVariable String code, HttpServletRequest req) {
        return this.roomService.subscribeToRoomUpdates(code);
    }
}
