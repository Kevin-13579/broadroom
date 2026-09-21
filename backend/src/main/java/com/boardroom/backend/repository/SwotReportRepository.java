package com.boardroom.backend.repository;

import com.boardroom.backend.model.SwotReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SwotReportRepository extends JpaRepository<SwotReport, Long> {
    Optional<SwotReport> findByRoomId(Long roomId);
    boolean existsByRoomId(Long roomId);
}
