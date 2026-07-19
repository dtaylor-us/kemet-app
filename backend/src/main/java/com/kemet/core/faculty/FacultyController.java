package com.kemet.core.faculty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kemet.core.domain.FacultyContent;
import com.kemet.core.faculty.dto.FacultySummary;
import com.kemet.core.repository.FacultyContentRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    private final FacultyContentRepository facultyContentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FacultyController(FacultyContentRepository facultyContentRepository) {
        this.facultyContentRepository = facultyContentRepository;
    }

    // No entitlement check here yet — REQ-004's public/course/chapter tiers are out of
    // scope for this single-user build (every authenticated user can read every seeded
    // faculty, and there's only one user anyway).
    @GetMapping
    public List<FacultySummary> listFaculties() {
        return facultyContentRepository.findAll().stream()
                .map(this::toSummary)
                .sorted(Comparator.comparingInt(FacultySummary::practiceOrder))
                .toList();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getFaculty(@PathVariable String id) {
        return facultyContentRepository.findById(id)
                .map(f -> ResponseEntity.ok(f.getContentJson()))
                .orElse(ResponseEntity.notFound().build());
    }

    private FacultySummary toSummary(FacultyContent content) {
        try {
            JsonNode node = objectMapper.readTree(content.getContentJson());
            return new FacultySummary(
                    content.getId(),
                    content.getDisplayName(),
                    node.path("role").asText(""),
                    node.path("practiceOrder").asInt(0));
        } catch (IOException e) {
            // Seeded content should always be well-formed JSON (SeedDataLoader wrote
            // it) — if this ever fires, something corrupted a row post-seed.
            throw new IllegalStateException("Corrupt faculty content for id=" + content.getId(), e);
        }
    }
}
