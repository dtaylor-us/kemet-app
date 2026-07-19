package com.kemet.core.faculty;

import com.kemet.core.domain.FacultyContent;
import com.kemet.core.repository.FacultyContentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FacultyControllerTest {
    private final FacultyContentRepository repository = mock(FacultyContentRepository.class);
    private final FacultyController controller = new FacultyController(repository);

    @Test
    void listsSummariesInPracticeOrder() {
        when(repository.findAll()).thenReturn(List.of(faculty("maat", "Ma'at", 5), faculty("amen", "Amen", 1)));
        var result = controller.listFaculties();
        assertThat(result).extracting("id").containsExactly("amen", "maat");
        assertThat(result.getFirst().role()).isEqualTo("foundation");
    }

    @Test
    void returnsFullFacultyJsonOrNotFound() {
        FacultyContent amen = faculty("amen", "Amen", 1);
        when(repository.findById("amen")).thenReturn(Optional.of(amen));
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThat(controller.getFaculty("amen").getBody()).isEqualTo(amen.getContentJson());
        assertThat(controller.getFaculty("missing").getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void reportsCorruptSeededContent() {
        FacultyContent corrupt = new FacultyContent();
        corrupt.setId("bad");
        corrupt.setDisplayName("Bad");
        corrupt.setContentJson("{");
        when(repository.findAll()).thenReturn(List.of(corrupt));
        assertThatThrownBy(controller::listFaculties)
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("id=bad");
    }

    private FacultyContent faculty(String id, String name, int order) {
        FacultyContent content = new FacultyContent();
        content.setId(id);
        content.setDisplayName(name);
        content.setContentJson("{\"role\":\"foundation\",\"practiceOrder\":" + order + "}");
        return content;
    }
}
