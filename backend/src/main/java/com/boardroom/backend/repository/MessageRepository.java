package com.boardroom.backend.repository;

import com.boardroom.backend.model.Message;
import com.boardroom.backend.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRoomOrderByTimestampAsc(Room room);
    List<Message> findByRoomIdOrderByTimestampAsc(Long roomId);
}
