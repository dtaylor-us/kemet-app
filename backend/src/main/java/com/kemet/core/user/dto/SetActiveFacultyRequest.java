package com.kemet.core.user.dto;

import jakarta.validation.constraints.NotBlank;

public record SetActiveFacultyRequest(@NotBlank String facultyId) {
}
