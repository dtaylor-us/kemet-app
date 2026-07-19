package com.kemet.core.companion.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message) {
}
