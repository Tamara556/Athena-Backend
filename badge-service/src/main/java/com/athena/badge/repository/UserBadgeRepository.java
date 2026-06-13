package com.athena.badge.repository;

import com.athena.badge.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    List<UserBadge> findByUserIdOrderByAwardedAtAsc(UUID userId);

    boolean existsByUserIdAndBadge_Code(UUID userId, String badgeCode);

    @org.springframework.data.jpa.repository.Query(
            "select ub.badge.code from UserBadge ub where ub.userId = :userId")
    Set<String> findEarnedCodesByUserId(UUID userId);
}
