package com.kemet.core.companion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kemet.core.domain.PracticeState;
import com.kemet.core.repository.ChatMessageRepository;
import com.kemet.core.repository.FacultyContentRepository;
import com.kemet.core.repository.PracticeStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CompanionPromptAdl027Adl028Test {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void adl027_systemPromptContainsEveryHardConstraintSentenceVerbatim() throws Exception {
        CompanionService service = serviceWithRealPromptTemplate();
        FacultySeed amen = loadFacultySeeds().get("amen");

        String assembledPrompt = buildSystemPrompt(service, amen.contentJson(), practiceStateFor("amen"));
        String promptTemplate = new String(new ClassPathResource("prompts/companion-system-prompt.md")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        String hardConstraintSection = extractSection(promptTemplate,
                "## HARD CONSTRAINTS — NEVER VIOLATE THESE",
                "## BEHAVIOR");

        assertThat(assembledPrompt)
                .withFailMessage("ADL-027 violated: assembled system prompt is missing content from HARD CONSTRAINTS section")
                .contains(hardConstraintSection);
    }

    @Test
    void adl028_promptContainsOnlyActiveFacultysHekauContent() throws Exception {
        CompanionService service = serviceWithRealPromptTemplate();
        Map<String, FacultySeed> seedsByFacultyId = loadFacultySeeds();

        for (FacultySeed active : seedsByFacultyId.values()) {
            String assembledPrompt = buildSystemPrompt(service, active.contentJson(), practiceStateFor(active.facultyId()));

            for (String ownHekau : active.hekauTexts()) {
                assertThat(assembledPrompt)
                        .withFailMessage("ADL-028 violated: prompt for faculty '%s' missing own hekau text: %s",
                                active.facultyId(), ownHekau)
                        .contains(ownHekau);
            }

            for (FacultySeed other : seedsByFacultyId.values()) {
                if (other.facultyId().equals(active.facultyId())) {
                    continue;
                }
                for (String otherHekau : other.hekauTexts()) {
                    if (active.hekauTexts().contains(otherHekau)) {
                        continue;
                    }
                    assertThat(assembledPrompt)
                            .withFailMessage("ADL-028 violated: prompt for faculty '%s' leaked hekau from faculty '%s': %s",
                                    active.facultyId(), other.facultyId(), otherHekau)
                            .doesNotContain(otherHekau);
                }
            }
        }
    }

    private CompanionService serviceWithRealPromptTemplate() {
        CompanionService service = new CompanionService(
                mock(ChatMessageRepository.class),
                mock(PracticeStateRepository.class),
                mock(FacultyContentRepository.class)
        );
        ReflectionTestUtils.setField(service, "systemPromptResource",
                new ClassPathResource("prompts/companion-system-prompt.md"));
        return service;
    }

    private Map<String, FacultySeed> loadFacultySeeds() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        var resources = resolver.getResources("classpath:seed/faculties/*.json");

        Map<String, FacultySeed> result = new HashMap<>();
        for (var resource : resources) {
            JsonNode root = objectMapper.readTree(resource.getInputStream());
            String facultyId = root.path("id").asText();
            List<String> hekauTexts = new ArrayList<>();
            for (JsonNode hekauNode : root.path("hekau")) {
                hekauTexts.add(hekauNode.path("text").asText());
            }
            result.put(facultyId, new FacultySeed(facultyId, objectMapper.writeValueAsString(root), List.copyOf(hekauTexts)));
        }

        return result;
    }

    private PracticeState practiceStateFor(String facultyId) {
        PracticeState state = new PracticeState();
        state.setFacultyId(facultyId);
        state.setCompletedDays(3);
        state.setLastPracticedAt(Instant.parse("2026-07-19T00:00:00Z"));
        state.setJournalAnalysisOptIn(false);
        return state;
    }

    private String buildSystemPrompt(CompanionService service, String facultyContentJson, PracticeState state) {
        return ReflectionTestUtils.invokeMethod(service, "buildSystemPrompt", facultyContentJson, state);
    }

    private static String extractSection(String markdown, String startHeading, String endHeading) {
        int start = markdown.indexOf(startHeading);
        int end = markdown.indexOf(endHeading);
        return markdown.substring(start, end).trim();
    }

    private record FacultySeed(String facultyId, String contentJson, List<String> hekauTexts) {
    }
}
