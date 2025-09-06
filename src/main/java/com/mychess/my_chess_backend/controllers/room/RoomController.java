package com.mychess.my_chess_backend.controllers.room;

import com.mychess.my_chess_backend.dtos.requests.room.JoinedRoomDTO;
import com.mychess.my_chess_backend.dtos.requests.room.JoiningRoomDTO;
import com.mychess.my_chess_backend.dtos.responses.BasicResponseDTO;
import com.mychess.my_chess_backend.models.Room;
import com.mychess.my_chess_backend.models.User;
import com.mychess.my_chess_backend.services.room.RoomService;
import com.mychess.my_chess_backend.services.user.UserService;
import com.mychess.my_chess_backend.utils.MyChessErrorHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/room")
@RestController
public class RoomController {
    private final RoomService roomService;
    private final UserService userService;

    public RoomController(
            RoomService roomService,
            UserService userService
    ) {
        this.roomService = roomService;
        this.userService = userService;
    }

    @PostMapping("/create")
    public ResponseEntity<BasicResponseDTO<Room>> createRoom(HttpServletRequest req) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User whitePlayer = (User) authentication.getPrincipal();
            Room newRoom = this.roomService.createRoom(whitePlayer);
            return ResponseEntity.ok(new BasicResponseDTO<>(
                    "success",
                    HttpStatus.OK.value(),
                    newRoom,
                    req.getRequestURI()
            ));
        } catch (Exception exception) {
            return MyChessErrorHandler.exceptionHandler("Failed creating a room. Try again !", req.getRequestURI());
        }
    }

    @PostMapping("/join")
    public ResponseEntity<BasicResponseDTO<JoinedRoomDTO>> joinRoom(HttpServletRequest req, @RequestBody JoiningRoomDTO roomDetails) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User blackPlayer = (User) authentication.getPrincipal();

            Room room = this.roomService.joinRoom(blackPlayer, roomDetails.getRoomId());
            if (room == null) {
                String errorMessage = "Cannot join room with roomId " + roomDetails.getRoomId();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new BasicResponseDTO<>(
                        errorMessage,
                        HttpStatus.NOT_FOUND.value(),
                        null,
                        req.getRequestURI()
                ));
            }

            return ResponseEntity.ok(new BasicResponseDTO<>(
                    "success",
                    HttpStatus.OK.value(),
                    new JoinedRoomDTO(
                            roomDetails.getRoomId(),
                            this.userService.getUsernameById(room.getWhitePlayer()),
                            blackPlayer.getUsername()
                    ),
                    req.getRequestURI()
            ));
        } catch (Exception exception) {
            return MyChessErrorHandler.exceptionHandler("Failed joining the room. Try again !", req.getRequestURI());
        }
    }
}
