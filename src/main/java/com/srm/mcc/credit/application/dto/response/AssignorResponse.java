package com.srm.mcc.credit.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AssignorResponse(
        UUID id,
        String name,
        String document,
        String email,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
