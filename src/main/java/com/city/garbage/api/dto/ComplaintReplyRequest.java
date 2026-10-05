package com.city.garbage.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ComplaintReplyRequest(@NotBlank String reply) {}
