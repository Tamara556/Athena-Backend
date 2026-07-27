package com.athena.rag.rag.repository;

import com.athena.rag.rag.entity.RagQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RagQueryLogRepository extends JpaRepository<RagQueryLog, UUID> {
}
