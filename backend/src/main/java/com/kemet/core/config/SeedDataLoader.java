package com.kemet.core.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kemet.core.domain.FacultyContent;
import com.kemet.core.repository.FacultyContentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Loads all 11 faculties into the database on startup, merging the shared
 * practice-framework.json (breathing technique, protocols, script, duration guidance,
 * and templated journal-prompt/daily-action text) into each faculty-specific file under
 * seed/faculties/*.json. This composition happens once, here, at seed time — the
 * *source* files stay DRY (framework written once, 11 small faculty files), even though
 * the persisted FacultyContent rows end up self-contained (each a fully merged blob),
 * which keeps FacultyController and CompanionService simple (no runtime merge logic
 * needed there).
 *
 * Stands in for a real teacher-authoring flow (REQ-019/020/021), which is out of scope
 * for this single-user prototype.
 */
@Component
public class SeedDataLoader implements CommandLineRunner {

    private final FacultyContentRepository facultyContentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.practice-framework-path}")
    private Resource practiceFrameworkResource;

    @Value("${app.seed.faculties-pattern}")
    private String facultiesPattern;

    public SeedDataLoader(FacultyContentRepository facultyContentRepository) {
        this.facultyContentRepository = facultyContentRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        JsonNode framework = objectMapper.readTree(
                practiceFrameworkResource.getInputStream().readAllBytes());

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] facultyResources = resolver.getResources(facultiesPattern);

        // Sort by filename (01-amen.json, 02-sekher.json, ...) so seeding order is
        // deterministic and matches practiceOrder — cosmetic only, doesn't affect
        // correctness, just makes logs/DB insert order easier to reason about.
        Arrays.sort(facultyResources, Comparator.comparing(Resource::getFilename));

        for (Resource facultyResource : facultyResources) {
            JsonNode facultyNode = objectMapper.readTree(
                    facultyResource.getInputStream().readAllBytes());
            String id = facultyNode.path("id").asText();

            if (facultyContentRepository.existsById(id)) {
                continue; // idempotent — don't overwrite content that may have local edits
            }

            ObjectNode merged = mergeFacultyWithFramework(facultyNode, framework);

            FacultyContent content = new FacultyContent();
            content.setId(id);
            content.setDisplayName(facultyNode.path("displayName").asText(id));
            content.setContentJson(objectMapper.writeValueAsString(merged));
            facultyContentRepository.save(content);
        }
    }

    private ObjectNode mergeFacultyWithFramework(JsonNode faculty, JsonNode framework) {
        ObjectNode merged = faculty.deepCopy();
        String facultyName = faculty.path("displayName").asText();

        merged.set("meditationInstructions", buildMeditationInstructions(framework));
        merged.set("journalPrompts", substituteFacultyName(
                (ArrayNode) framework.path("journalPromptsTemplate"), facultyName));
        merged.set("suggestedDailyActions", substituteFacultyName(
                (ArrayNode) framework.path("suggestedDailyActionsTemplate"), facultyName));
        merged.set("dailyTrackerFields", framework.path("dailyTrackerFields").deepCopy());

        return merged;
    }

    private ObjectNode buildMeditationInstructions(JsonNode framework) {
        ObjectNode instructions = objectMapper.createObjectNode();
        instructions.set("breathingTechnique", framework.path("breathingTechnique").deepCopy());
        instructions.set("environmentProtocols", framework.path("environmentProtocols").deepCopy());
        instructions.set("expectedSensations", framework.path("expectedSensations").deepCopy());
        instructions.set("scriptSteps", framework.path("scriptSteps").deepCopy());
        instructions.set("durationGuidance", framework.path("durationGuidance").deepCopy());
        return instructions;
    }

    private ArrayNode substituteFacultyName(ArrayNode template, String facultyName) {
        ArrayNode result = objectMapper.createArrayNode();
        Iterator<JsonNode> it = template.elements();
        while (it.hasNext()) {
            String text = it.next().asText().replace("{{FACULTY}}", facultyName);
            result.add(text);
        }
        return result;
    }
}
