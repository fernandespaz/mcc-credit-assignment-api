package com.srm.mcc.credit.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public class Assignor {

    private final UUID id;
    private final String name;
    private final String document; // CPF or CNPJ
    private final String email;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Assignor deactivate() {
        return this.withActive(false).withUpdatedAt(LocalDateTime.now());
    }

    public Assignor activate() {
        return this.withActive(true).withUpdatedAt(LocalDateTime.now());
    }
}
