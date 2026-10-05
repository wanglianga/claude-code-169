package com.city.garbage.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteTaskRequest(@NotBlank String note) {}
