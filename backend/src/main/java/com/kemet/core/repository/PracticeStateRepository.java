package com.kemet.core.repository;

import com.kemet.core.domain.PracticeState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PracticeStateRepository extends JpaRepository<PracticeState, UUID> {
    Optional<PracticeState> findByUserIdAndFacultyId(UUID userId, String facultyId);
}
