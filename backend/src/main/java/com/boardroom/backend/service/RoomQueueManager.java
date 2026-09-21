package com.boardroom.backend.service;

import com.boardroom.backend.dto.MessageDto;
import com.boardroom.backend.dto.QueueUserDto;
import com.boardroom.backend.dto.TurnEvent;
import com.boardroom.backend.model.Message;
import com.boardroom.backend.model.Room;
import com.boardroom.backend.model.RoomStatus;
import com.boardroom.backend.model.User;
import com.boardroom.backend.repository.MessageRepository;
import com.boardroom.backend.repository.RoomRepository;
import com.boardroom.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class RoomQueueManager {

    private static final Logger logger = LoggerFactory.getLogger(RoomQueueManager.class);
    private static final int TURN_DURATION_SECONDS = 45;

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;

    // Per-room bidding queue (User IDs)
    private final ConcurrentMap<Long, ConcurrentLinkedQueue<Long>> roomQueues = new ConcurrentHashMap<>();

    // Per-room current active turn state
    private final ConcurrentMap<Long, ActiveTurnState> activeTurns = new ConcurrentHashMap<>();

    // Scheduler for the 45-second speaker timer
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public static class ActiveTurnState {
        Long userId;
        String username;
        long expiresAt;
        ScheduledFuture<?> timerFuture;

        public ActiveTurnState(Long userId, String username, long expiresAt, ScheduledFuture<?> timerFuture) {
            this.userId = userId;
            this.username = username;
            this.expiresAt = expiresAt;
            this.timerFuture = timerFuture;
        }
    }

    public RoomQueueManager(SimpMessagingTemplate messagingTemplate,
                            UserRepository userRepository,
                            RoomRepository roomRepository,
                            MessageRepository messageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * User clicks "Bid to Speak"
     */
    public synchronized void addBid(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null || room.getStatus() != RoomStatus.ACTIVE) {
            logger.warn("Cannot bid on room {} with status {}", roomId, room != null ? room.getStatus() : "null");
            return;
        }

        ConcurrentLinkedQueue<Long> queue = roomQueues.computeIfAbsent(roomId, k -> new ConcurrentLinkedQueue<>());
        ActiveTurnState currentTurn = activeTurns.get(roomId);

        // Check if user is already the active speaker or already in the queue
        if (currentTurn != null && Objects.equals(currentTurn.userId, userId)) {
            logger.info("User {} is already speaking in room {}", userId, roomId);
            return;
        }

        if (queue.contains(userId)) {
            logger.info("User {} is already in bidding queue for room {}", userId, roomId);
            return;
        }

        queue.add(userId);
        logger.info("User {} added to bidding queue for room {}. Queue size: {}", userId, roomId, queue.size());

        // If no one is currently speaking, start turn immediately
        if (currentTurn == null || currentTurn.userId == null) {
            startNextTurn(roomId);
        } else {
            // Broadcast updated queue state
            broadcastTurnState(roomId);
        }
    }

    /**
     * Start the turn for the next user in the queue
     */
    public synchronized void startNextTurn(Long roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null || room.getStatus() != RoomStatus.ACTIVE) {
            clearRoom(roomId);
            return;
        }

        ConcurrentLinkedQueue<Long> queue = roomQueues.computeIfAbsent(roomId, k -> new ConcurrentLinkedQueue<>());

        // Cancel existing timer if any
        ActiveTurnState oldState = activeTurns.remove(roomId);
        if (oldState != null && oldState.timerFuture != null && !oldState.timerFuture.isDone()) {
            oldState.timerFuture.cancel(false);
        }

        Long nextUserId = queue.poll();

        if (nextUserId == null) {
            // Queue is empty, open floor
            logger.info("Room {} bidding queue is now empty. Floor open.", roomId);
            activeTurns.put(roomId, new ActiveTurnState(null, null, 0L, null));
            broadcastTurnState(roomId);
            return;
        }

        User user = userRepository.findById(nextUserId).orElse(null);
        if (user == null) {
            logger.warn("User {} not found, skipping to next in queue", nextUserId);
            startNextTurn(roomId);
            return;
        }

        long expiresAt = System.currentTimeMillis() + (TURN_DURATION_SECONDS * 1000L);

        // Schedule the 45-second timeout task
        ScheduledFuture<?> timerFuture = scheduler.schedule(() -> {
            handleTimeout(roomId, nextUserId);
        }, TURN_DURATION_SECONDS, TimeUnit.SECONDS);

        ActiveTurnState newState = new ActiveTurnState(user.getId(), user.getUsername(), expiresAt, timerFuture);
        activeTurns.put(roomId, newState);

        logger.info("Turn started for user {} in room {}, expires at {}", user.getUsername(), roomId, expiresAt);
        broadcastTurnState(roomId);
    }

    /**
     * Handles 45-second timeout when user does not speak
     */
    private synchronized void handleTimeout(Long roomId, Long expectedUserId) {
        ActiveTurnState currentTurn = activeTurns.get(roomId);
        if (currentTurn != null && Objects.equals(currentTurn.userId, expectedUserId)) {
            logger.info("Speaker {} timed out (45s elapsed) in room {}", currentTurn.username, roomId);
            
            // Broadcast timeout notification
            TurnEvent timeoutEvent = new TurnEvent(roomId, currentTurn.userId, currentTurn.username, 0L, getQueueUsers(roomId), "TIME_UP");
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/turn", timeoutEvent);

            // Advance immediately to next speaker
            startNextTurn(roomId);
        }
    }

    /**
     * Active speaker submits a message
     */
    public synchronized MessageDto postMessage(Long roomId, Long userId, String content) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new IllegalStateException("Room is not active");
        }

        ActiveTurnState currentTurn = activeTurns.get(roomId);
        if (currentTurn == null || !Objects.equals(currentTurn.userId, userId)) {
            throw new IllegalStateException("It is not your turn to speak");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Persist message
        Message message = new Message();
        message.setRoom(room);
        message.setUser(user);
        message.setContent(content.trim());
        message.setTimestamp(LocalDateTime.now());
        Message saved = messageRepository.save(message);

        MessageDto messageDto = new MessageDto(
                saved.getId(),
                roomId,
                user.getId(),
                user.getUsername(),
                saved.getContent(),
                saved.getTimestamp()
        );

        // Broadcast message to room topic
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/messages", messageDto);
        logger.info("Message saved and broadcasted from {} in room {}", user.getUsername(), roomId);

        // Turn fulfilled: cancel timer and trigger next turn immediately
        startNextTurn(roomId);

        return messageDto;
    }

    /**
     * Host starts the session and takes the first 45s speaking turn
     */
    public synchronized void startHostFirstTurn(Long roomId, Long hostUserId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null || room.getStatus() != RoomStatus.ACTIVE) {
            return;
        }

        // Cancel existing timer if any
        ActiveTurnState oldState = activeTurns.remove(roomId);
        if (oldState != null && oldState.timerFuture != null && !oldState.timerFuture.isDone()) {
            oldState.timerFuture.cancel(false);
        }

        User host = userRepository.findById(hostUserId).orElse(null);
        if (host == null) {
            startNextTurn(roomId);
            return;
        }

        long expiresAt = System.currentTimeMillis() + (TURN_DURATION_SECONDS * 1000L);

        ScheduledFuture<?> timerFuture = scheduler.schedule(() -> {
            handleTimeout(roomId, hostUserId);
        }, TURN_DURATION_SECONDS, TimeUnit.SECONDS);

        ActiveTurnState newState = new ActiveTurnState(host.getId(), host.getUsername(), expiresAt, timerFuture);
        activeTurns.put(roomId, newState);

        logger.info("Host {} started first turn in room {}, expires at {}", host.getUsername(), roomId, expiresAt);
        broadcastTurnState(roomId);
    }

    /**
     * Remove user from bidding queue and advance turn if they were currently speaking
     */
    public synchronized void removeUserFromQueueAndTurn(Long roomId, Long userId) {
        ConcurrentLinkedQueue<Long> queue = roomQueues.get(roomId);
        if (queue != null) {
            queue.remove(userId);
        }

        ActiveTurnState currentTurn = activeTurns.get(roomId);
        if (currentTurn != null && Objects.equals(currentTurn.userId, userId)) {
            logger.info("Current speaker {} left room {}. Advancing turn.", userId, roomId);
            if (currentTurn.timerFuture != null && !currentTurn.timerFuture.isDone()) {
                currentTurn.timerFuture.cancel(false);
            }
            startNextTurn(roomId);
        } else {
            broadcastTurnState(roomId);
        }
    }

    /**
     * Broadcasts current turn and queue state
     */
    public void broadcastTurnState(Long roomId) {
        ActiveTurnState current = activeTurns.get(roomId);
        List<QueueUserDto> queueUsers = getQueueUsers(roomId);

        TurnEvent event = new TurnEvent();
        event.setRoomId(roomId);
        event.setQueue(queueUsers);
        roomRepository.findById(roomId).ifPresent(r -> event.setRoomStatus(r.getStatus().name()));

        if (current != null && current.userId != null) {
            event.setActiveUserId(current.userId);
            event.setUsername(current.username);
            event.setExpiresAt(current.expiresAt);
            event.setEventType("TURN_ACTIVE");
        } else {
            event.setActiveUserId(null);
            event.setUsername(null);
            event.setExpiresAt(null);
            event.setEventType("FLOOR_OPEN");
        }

        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/turn", event);
    }

    /**
     * Retrieve current turn state for a room (e.g. for newly connecting users)
     */
    public TurnEvent getCurrentTurnState(Long roomId) {
        ActiveTurnState current = activeTurns.get(roomId);
        List<QueueUserDto> queueUsers = getQueueUsers(roomId);

        TurnEvent event = new TurnEvent();
        event.setRoomId(roomId);
        event.setQueue(queueUsers);
        roomRepository.findById(roomId).ifPresent(r -> event.setRoomStatus(r.getStatus().name()));

        if (current != null && current.userId != null) {
            event.setActiveUserId(current.userId);
            event.setUsername(current.username);
            event.setExpiresAt(current.expiresAt);
            event.setEventType("TURN_ACTIVE");
        } else {
            event.setActiveUserId(null);
            event.setUsername(null);
            event.setExpiresAt(null);
            event.setEventType("FLOOR_OPEN");
        }
        return event;
    }

    private List<QueueUserDto> getQueueUsers(Long roomId) {
        ConcurrentLinkedQueue<Long> queue = roomQueues.get(roomId);
        if (queue == null || queue.isEmpty()) {
            return Collections.emptyList();
        }

        List<QueueUserDto> dtos = new ArrayList<>();
        for (Long uid : queue) {
            userRepository.findById(uid).ifPresent(u -> dtos.add(new QueueUserDto(u.getId(), u.getUsername())));
        }
        return dtos;
    }

    /**
     * Clear queue and cancel timers when room ends
     */
    public synchronized void clearRoom(Long roomId) {
        ActiveTurnState state = activeTurns.remove(roomId);
        if (state != null && state.timerFuture != null && !state.timerFuture.isDone()) {
            state.timerFuture.cancel(false);
        }
        roomQueues.remove(roomId);
    }
}
