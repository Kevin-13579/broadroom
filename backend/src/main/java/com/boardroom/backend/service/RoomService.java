package com.boardroom.backend.service;

import com.boardroom.backend.dto.*;
import com.boardroom.backend.model.*;
import com.boardroom.backend.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public RoomService(RoomRepository roomRepository,
                       RoomMemberRepository roomMemberRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (roomRepository.existsByRoomCode(code));
        return code;
    }

    @Transactional
    public RoomDto createRoom(CreateRoomRequest request, Long hostUserId) {
        User host = userRepository.findById(hostUserId)
                .orElseThrow(() -> new IllegalArgumentException("Host user not found"));

        if (request.getMaxMembers() < 3 || request.getMaxMembers() > 10) {
            throw new IllegalArgumentException("Maximum participants must be between 3 and 10");
        }

        Room room = new Room();
        room.setRoomCode(generateUniqueRoomCode());
        room.setTopic(request.getTopic().trim());
        room.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        room.setHost(host);
        room.setMaxMembers(request.getMaxMembers());
        room.setStatus(RoomStatus.LOBBY);
        room.setCreatedAt(LocalDateTime.now());

        Room savedRoom = roomRepository.save(room);

        // Add host as the first member
        RoomMember member = new RoomMember();
        member.setRoom(savedRoom);
        member.setUser(host);
        member.setJoinedAt(LocalDateTime.now());
        roomMemberRepository.save(member);

        return toRoomDto(savedRoom, hostUserId);
    }

    @Transactional
    public RoomDto joinRoom(JoinRoomRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String roomCode = request.getRoomCode().trim().toUpperCase();
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with code: " + roomCode));

        if (!passwordEncoder.matches(request.getPassword().trim(), room.getPassword())) {
            throw new IllegalArgumentException("Invalid room password");
        }

        boolean alreadyMember = roomMemberRepository.existsByRoomIdAndUserId(room.getId(), userId);

        if (!alreadyMember) {
            if (room.getStatus() != RoomStatus.LOBBY) {
                throw new IllegalStateException("Cannot join room that has already started or ended");
            }

            int currentMemberCount = roomMemberRepository.countByRoomId(room.getId());
            if (currentMemberCount >= room.getMaxMembers()) {
                throw new IllegalStateException("Room is already full (maximum " + room.getMaxMembers() + " participants)");
            }

            RoomMember newMember = new RoomMember();
            newMember.setRoom(room);
            newMember.setUser(user);
            newMember.setJoinedAt(LocalDateTime.now());
            roomMemberRepository.save(newMember);
        }

        return toRoomDto(room, userId);
    }

    @Transactional(readOnly = true)
    public RoomDto getRoom(Long roomId, Long currentUserId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));
        return toRoomDto(room, currentUserId);
    }

    @Transactional(readOnly = true)
    public RoomDto getRoomByCode(String roomCode, Long currentUserId) {
        Room room = roomRepository.findByRoomCode(roomCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Room not found with code: " + roomCode));
        return toRoomDto(room, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<RoomMemberDto> getRoomMembers(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));

        List<RoomMember> members = roomMemberRepository.findByRoomId(roomId);
        Long hostId = room.getHost().getId();

        return members.stream().map(m -> new RoomMemberDto(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getUsername(),
                m.getUser().getEmail(),
                m.getUser().getId().equals(hostId),
                m.getJoinedAt()
        )).collect(Collectors.toList());
    }

    @Transactional
    public RoomDto startRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!room.getHost().getId().equals(userId)) {
            throw new IllegalStateException("Only the room host can start the session");
        }

        if (room.getStatus() == RoomStatus.ENDED) {
            throw new IllegalStateException("Session has already ended");
        }

        room.setStatus(RoomStatus.ACTIVE);
        Room updated = roomRepository.save(room);
        return toRoomDto(updated, userId);
    }

    @Transactional
    public RoomDto leaveRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));

        roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .ifPresent(roomMemberRepository::delete);

        List<RoomMember> remainingMembers = roomMemberRepository.findByRoomId(roomId);
        if (remainingMembers.isEmpty()) {
            room.setStatus(RoomStatus.ENDED);
            Room saved = roomRepository.save(room);
            return toRoomDto(saved, userId);
        }

        // If the user who left was the host, reassign host to the first remaining member
        if (room.getHost().getId().equals(userId)) {
            User newHost = remainingMembers.get(0).getUser();
            room.setHost(newHost);
            room = roomRepository.save(room);
        }

        return toRoomDto(room, userId);
    }

    public RoomDto toRoomDto(Room room, Long currentUserId) {
        int count = roomMemberRepository.countByRoomId(room.getId());
        User host = room.getHost();
        UserDto hostDto = new UserDto(host.getId(), host.getUsername(), host.getEmail());
        boolean isHost = host.getId().equals(currentUserId);

        return new RoomDto(
                room.getId(),
                room.getRoomCode(),
                room.getTopic(),
                hostDto,
                room.getMaxMembers(),
                room.getStatus(),
                count,
                room.getCreatedAt(),
                isHost
        );
    }
}
