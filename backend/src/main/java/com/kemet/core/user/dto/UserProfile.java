package com.kemet.core.user.dto;

import java.util.UUID;

public record UserProfile(UUID id, String displayName, String activeFacultyId) {
}
