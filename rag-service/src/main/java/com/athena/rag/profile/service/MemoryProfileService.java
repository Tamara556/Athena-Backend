package com.athena.rag.profile.service;

import com.athena.rag.profile.dto.MemoryProfileResponse;

import java.util.UUID;

public interface MemoryProfileService {

    MemoryProfileResponse getProfile(UUID userId);
}
