package com.city.garbage.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignTaskRequest(@NotBlank String assigneeName) {}
