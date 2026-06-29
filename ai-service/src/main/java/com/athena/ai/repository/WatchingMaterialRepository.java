package com.athena.ai.repository;

import com.athena.ai.entity.WatchingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WatchingMaterialRepository extends JpaRepository<WatchingMaterial, UUID> {

    List<WatchingMaterial> findBySessionIdOrderByOrderIndexAsc(UUID sessionId);
}
