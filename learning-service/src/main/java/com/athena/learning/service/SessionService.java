package com.athena.learning.service;

import com.athena.learning.dto.EndSessionRequest;
import com.athena.learning.dto.SessionResponse;
import com.athena.learning.dto.StartSessionRequest;

import java.util.UUID;

public interface SessionService {

    SessionResponse start(UUID userId, StartSessionRequest request);

    SessionResponse end(EndSessionRequest request);
}
