package com.athena.learning.service;

import com.athena.learning.dto.CreateTaskRequest;
import com.athena.learning.dto.TaskResponse;

import java.util.UUID;

public interface TaskService {

    TaskResponse create(CreateTaskRequest request);

    TaskResponse getById(UUID taskId);

    TaskResponse complete(UUID taskId);
}
