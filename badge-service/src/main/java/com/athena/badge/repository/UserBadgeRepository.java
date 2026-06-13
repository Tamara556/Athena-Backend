package com.athena.badge.repository;

import com.athena.badge.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    List<UserBadge> findByUserIdOrderByAwardedAtAsc(UUID userId);

    @Query(
            "select ub.badge.code from UserBadge ub where ub.userId = :userId")
    Set<String> findEarnedCodesByUserId(UUID userId);
}
