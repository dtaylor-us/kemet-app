package com.kemet.core.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kemet.core.domain.FacultyContent;
import com.kemet.core.repository.FacultyContentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SeedDataLoaderTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void seedsAllFacultiesWithSharedFramework() throws Exception {
        FacultyContentRepository repository = mock(FacultyContentRepository.class);
        SeedDataLoader loader = configuredLoader(repository);

        loader.run();

        var captor = org.mockito.ArgumentCaptor.forClass(FacultyContent.class);
        verify(repository, times(11)).save(captor.capture());
        FacultyContent amen = captor.getAllValues().getFirst();
        JsonNode content = mapper.readTree(amen.getContentJson());
        assertThat(amen.getId()).isEqualTo("amen");
        assertThat(content.path("meditationInstructions").path("scriptSteps").isArray()).isTrue();
        assertThat(content.path("journalPrompts").toString()).contains("Amen").doesNotContain("{{FACULTY}}");
        assertThat(content.path("dailyTrackerFields").isArray()).isTrue();
    }

    @Test
    void leavesAlreadySeededFacultyUntouched() throws Exception {
        FacultyContentRepository repository = mock(FacultyContentRepository.class);
        when(repository.existsById(any())).thenReturn(true);

        configuredLoader(repository).run();

        verify(repository, never()).save(any());
    }

    private SeedDataLoader configuredLoader(FacultyContentRepository repository) {
        SeedDataLoader loader = new SeedDataLoader(repository);
        ReflectionTestUtils.setField(loader, "practiceFrameworkResource",
                new ClassPathResource("seed/practice-framework.json"));
        ReflectionTestUtils.setField(loader, "facultiesPattern", "classpath:seed/faculties/*.json");
        return loader;
    }
}
