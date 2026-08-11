package com.srm.mcc.credit.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAssignorRequest(
        @Size(max = 150)
        String name,

        @Email(message = "Invalid email format")
        String email
) {}
