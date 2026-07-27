package com.athena.ai.learningsession.repository;

import com.athena.ai.learningsession.entity.ReadingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReadingMaterialRepository extends JpaRepository<ReadingMaterial, UUID> {

    List<ReadingMaterial> findBySessionIdOrderByOrderIndexAsc(UUID sessionId);
}
