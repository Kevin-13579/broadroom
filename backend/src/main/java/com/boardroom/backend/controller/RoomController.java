package com.boardroom.backend.controller;

import com.boardroom.backend.dto.*;
import com.boardroom.backend.model.Message;
import com.boardroom.backend.repository.MessageRepository;
import com.boardroom.backend.security.UserPrincipal;
import com.boardroom.backend.service.RoomQueueManager;
import com.boardroom.backend.service.RoomService;
import com.boardroom.backend.service.SwotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoomQueueManager roomQueueManager;
    private final SwotService swotService;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomController(RoomService roomService,
                          RoomQueueManager roomQueueManager,
                          SwotService swotService,
                          MessageRepository messageRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.roomQueueManager = roomQueueManager;
        this.swotService = swotService;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createRoom(@Valid @RequestBody CreateRoomRequest request,
                                        @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not authenticated"));
            }
            RoomDto roomDto = roomService.createRoom(request, currentUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(roomDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to create room: " + e.getMessage()));
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(@Valid @RequestBody JoinRoomRequest request,
                                      @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not authenticated"));
            }
            RoomDto roomDto = roomService.joinRoom(request, currentUser.getId());
            return ResponseEntity.ok(roomDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to join room: " + e.getMessage()));
        }
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable Long roomId,
                                     @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Long userId = currentUser != null ? currentUser.getId() : -1L;
            RoomDto roomDto = roomService.getRoom(roomId, userId);
            return ResponseEntity.ok(roomDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{roomId}/members")
    public ResponseEntity<?> getRoomMembers(@PathVariable Long roomId) {
        try {
            List<RoomMemberDto> members = roomService.getRoomMembers(roomId);
            return ResponseEntity.ok(members);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/start")
    public ResponseEntity<?> startRoom(@PathVariable Long roomId,
                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not authenticated"));
            }
            RoomDto roomDto = roomService.startRoom(roomId, currentUser.getId());
            // Broadcast room status change to all participants
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/status", roomDto);
            // Start host as the first speaker (45 seconds)
            roomQueueManager.startHostFirstTurn(roomId, currentUser.getId());
            return ResponseEntity.ok(roomDto);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to start session: " + e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable Long roomId,
                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not authenticated"));
            }
            RoomDto updated = roomService.leaveRoom(roomId, currentUser.getId());
            roomQueueManager.removeUserFromQueueAndTurn(roomId, currentUser.getId());

            // Broadcast updated member list to room topic
            List<RoomMemberDto> members = roomService.getRoomMembers(roomId);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/members", members);

            // Broadcast updated room state
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/status", updated);

            return ResponseEntity.ok(Map.of("message", "Successfully left room"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to leave room: " + e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/end")
    public ResponseEntity<?> endRoomAndGenerateSwot(@PathVariable Long roomId,
                                                    @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not authenticated"));
            }
            SwotResponseDto swot = swotService.endRoomAndGenerateSwot(roomId, currentUser.getId());
            try {
                RoomDto roomDto = roomService.getRoom(roomId, currentUser.getId());
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/status", roomDto);
            } catch (Exception ignored) {}
            return ResponseEntity.ok(swot);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to generate SWOT: " + e.getMessage()));
        }
    }

    @GetMapping("/{roomId}/swot")
    public ResponseEntity<?> getSwot(@PathVariable Long roomId) {
        try {
            SwotResponseDto swot = swotService.getSwotReport(roomId);
            return ResponseEntity.ok(swot);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{roomId}/turn")
    public ResponseEntity<?> getTurnState(@PathVariable Long roomId) {
        TurnEvent turnEvent = roomQueueManager.getCurrentTurnState(roomId);
        return ResponseEntity.ok(turnEvent);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long roomId) {
        List<Message> messages = messageRepository.findByRoomIdOrderByTimestampAsc(roomId);
        List<MessageDto> dtos = messages.stream().map(m -> new MessageDto(
                m.getId(),
                roomId,
                m.getUser().getId(),
                m.getUser().getUsername(),
                m.getContent(),
                m.getTimestamp()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
