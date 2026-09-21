package com.boardroom.backend.repository;

import com.boardroom.backend.model.Room;
import com.boardroom.backend.model.RoomMember;
import com.boardroom.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    List<RoomMember> findByRoom(Room room);
    List<RoomMember> findByRoomId(Long roomId);
    Optional<RoomMember> findByRoomAndUser(Room room, User user);
    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);
    int countByRoom(Room room);
    int countByRoomId(Long roomId);
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
}
