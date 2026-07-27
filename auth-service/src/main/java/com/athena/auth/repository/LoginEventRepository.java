package com.athena.auth.repository;

import com.athena.auth.entity.LoginEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoginEventRepository extends JpaRepository<LoginEvent, UUID> {

    List<LoginEvent> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);
}
