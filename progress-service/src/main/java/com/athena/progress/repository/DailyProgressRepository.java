package com.athena.progress.repository;

import com.athena.progress.entity.DailyProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyProgressRepository extends JpaRepository<DailyProgress, UUID> {

    Optional<DailyProgress> findByUserIdAndDate(UUID userId, LocalDate date);

    List<DailyProgress> findByUserIdAndDateBetweenOrderByDateAsc(UUID userId, LocalDate start, LocalDate end);
}
