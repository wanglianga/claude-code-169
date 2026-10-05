package com.city.garbage.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ComplaintRequest(
        Long residentId,
        String contact,
        @NotBlank String roomCode,
        String type,
        @NotBlank String content
) {}
