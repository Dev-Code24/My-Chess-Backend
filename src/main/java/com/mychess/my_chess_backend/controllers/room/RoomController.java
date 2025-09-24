package com.mychess.my_chess_backend.controllers.room;

import com.mychess.my_chess_backend.dtos.requests.room.JoiningRoomDTO;
import com.mychess.my_chess_backend.dtos.requests.room.PieceMovedDTO;
import com.mychess.my_chess_backend.dtos.responses.BasicResponseDTO;
import com.mychess.my_chess_backend.dtos.responses.room.RoomDTO;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.services.room.RoomService;
import com.mychess.my_chess_backend.utils.MyChessErrorHandler;
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
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User whitePlayer = (User) authentication.getPrincipal();
            RoomDTO newRoom = this.roomService.createRoom(whitePlayer);
            return ResponseEntity.ok(new BasicResponseDTO<>(
                    "success",
                    HttpStatus.OK.value(),
                    newRoom,
                    req.getRequestURI()
            ));
        } catch (Exception exception) {
            return MyChessErrorHandler.exceptionHandler(exception.getMessage(), req.getRequestURI());
        }
    }

    @PostMapping("/join")
    public ResponseEntity<BasicResponseDTO<RoomDTO>> joinRoom(
            HttpServletRequest req,
            @RequestBody JoiningRoomDTO roomDetails
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User blackPlayer = (User) authentication.getPrincipal();

            RoomDTO room = this.roomService.joinRoom(blackPlayer, roomDetails.getCode());
            if (room == null) {
                String message = "Cannot join room with roomId " + roomDetails.getCode();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new BasicResponseDTO<>(
                        message,
                        HttpStatus.NOT_FOUND.value(),
                        null,
                        req.getRequestURI()
                ));
            }

            return ResponseEntity.ok(new BasicResponseDTO<>(
                    "success",
                    HttpStatus.OK.value(),
                    room,
                    req.getRequestURI()
            ));
        } catch (Exception exception) {
            return MyChessErrorHandler.exceptionHandler(exception.getMessage(), req.getRequestURI());
        }
    }

    @PostMapping("/{code}")
    public ResponseEntity<BasicResponseDTO<String>> pieceMoved(
            @PathVariable("code") String code,
            @RequestBody PieceMovedDTO pieceMoved,
            HttpServletRequest req
    ) {
        // Use service to broadcast to room
        this.roomService.pieceMoved(pieceMoved, code);
        return ResponseEntity.ok(new BasicResponseDTO<>(
                "success",
                HttpStatus.OK.value(),
                "Updated Piece",
                req.getRequestURI()
        ));
    }

    @GetMapping("/{code}")
    public ResponseEntity<BasicResponseDTO<RoomDTO>> roomDetails(
            @PathVariable("code") String code,
            HttpServletRequest req
    ) {
        try {
            RoomDTO room = this.roomService.getRoom(code);
            if (room == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(new BasicResponseDTO<>(
                    "success",
                    HttpStatus.OK.value(),
                    room,
                    req.getRequestURI()
            ));
        } catch (Exception exception) {
            return MyChessErrorHandler.exceptionHandler(exception.getMessage(), req.getRequestURI());
        }
    }

    @GetMapping("/live/{code}")
    public SseEmitter streamEvents(@PathVariable String code, HttpServletRequest req) {
        return this.roomService.subscribeToRoomUpdates(code);
    }
}
