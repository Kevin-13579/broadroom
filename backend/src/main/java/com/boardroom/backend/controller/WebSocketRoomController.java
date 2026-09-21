package com.boardroom.backend.controller;

import com.boardroom.backend.dto.RoomMemberDto;
import com.boardroom.backend.service.RoomQueueManager;
import com.boardroom.backend.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
public class WebSocketRoomController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketRoomController.class);

    private final RoomQueueManager roomQueueManager;
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketRoomController(RoomQueueManager roomQueueManager,
                                   RoomService roomService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.roomQueueManager = roomQueueManager;
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * User bids to speak: client sends to /app/room/{roomId}/bid
     */
    @MessageMapping("/room/{roomId}/bid")
    public void handleBid(@DestinationVariable Long roomId, @Payload Map<String, Object> payload) {
        try {
            Number userIdNum = (Number) payload.get("userId");
            if (userIdNum == null) {
                logger.warn("Received bid without userId for room {}", roomId);
                return;
            }
            Long userId = userIdNum.longValue();
            logger.info("Handling bid for room {} from user {}", roomId, userId);
            roomQueueManager.addBid(roomId, userId);
        } catch (Exception e) {
            logger.error("Error processing bid in room {}: {}", roomId, e.getMessage());
        }
    }

    /**
     * Active speaker posts message: client sends to /app/room/{roomId}/speak
     */
    @MessageMapping("/room/{roomId}/speak")
    public void handleSpeak(@DestinationVariable Long roomId, @Payload Map<String, Object> payload) {
        try {
            Number userIdNum = (Number) payload.get("userId");
            String content = (String) payload.get("content");
            if (userIdNum == null || content == null || content.trim().isEmpty()) {
                logger.warn("Invalid speak payload in room {}", roomId);
                return;
            }
            Long userId = userIdNum.longValue();
            logger.info("Handling speak in room {} from user {}: {}", roomId, userId, content);
            roomQueueManager.postMessage(roomId, userId, content);
        } catch (Exception e) {
            logger.error("Error processing speak in room {}: {}", roomId, e.getMessage());
        }
    }

    /**
     * Request current state sync: client sends to /app/room/{roomId}/sync
     */
    @MessageMapping("/room/{roomId}/sync")
    public void handleSync(@DestinationVariable Long roomId) {
        roomQueueManager.broadcastTurnState(roomId);
    }

    /**
     * User leaves room via WebSocket: client sends to /app/room/{roomId}/leave
     */
    @MessageMapping("/room/{roomId}/leave")
    public void handleLeave(@DestinationVariable Long roomId, @Payload Map<String, Object> payload) {
        try {
            Number userIdNum = (Number) payload.get("userId");
            if (userIdNum != null) {
                Long userId = userIdNum.longValue();
                logger.info("Handling WebSocket leave for user {} in room {}", userId, roomId);
                roomService.leaveRoom(roomId, userId);
                roomQueueManager.removeUserFromQueueAndTurn(roomId, userId);

                List<RoomMemberDto> members = roomService.getRoomMembers(roomId);
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/members", members);
            }
        } catch (Exception e) {
            logger.error("Error handling WebSocket leave for room {}: {}", roomId, e.getMessage());
        }
    }
}
