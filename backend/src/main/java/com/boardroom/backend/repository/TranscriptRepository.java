package com.boardroom.backend.repository;

import com.boardroom.backend.model.Transcript;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TranscriptRepository extends JpaRepository<Transcript, Long> {
    List<Transcript> findBySessionIdOrderByTimestampAsc(Long sessionId);
}