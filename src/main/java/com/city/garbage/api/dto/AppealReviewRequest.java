package com.city.garbage.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AppealReviewRequest(boolean accepted, @NotBlank String reviewNote) {}
