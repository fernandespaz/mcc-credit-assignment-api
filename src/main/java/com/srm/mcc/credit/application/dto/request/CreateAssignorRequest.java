package com.srm.mcc.credit.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAssignorRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150)
        String name,

        @NotBlank(message = "Document (CPF/CNPJ) is required")
        @Pattern(regexp = "\\d{11}|\\d{14}", message = "Document must be 11 (CPF) or 14 (CNPJ) digits")
        String document,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
) {}
