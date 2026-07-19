package com.kemet.core.companion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kemet.core.domain.AppUser;
import com.kemet.core.domain.ChatMessage;
import com.kemet.core.domain.FacultyContent;
import com.kemet.core.domain.PracticeState;
import com.kemet.core.repository.ChatMessageRepository;
import com.kemet.core.repository.FacultyContentRepository;
import com.kemet.core.repository.PracticeStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompanionServiceTest {
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final PracticeStateRepository states = mock(PracticeStateRepository.class);
    private final FacultyContentRepository faculties = mock(FacultyContentRepository.class);
    private final HttpClient client = mock(HttpClient.class);
    private final CompanionService service = new CompanionService(messages, states, faculties,
            new ObjectMapper(), client);
    private final AppUser user = new AppUser();

    @BeforeEach
    void setUp() {
        user.setId(UUID.randomUUID());
        user.setActiveFacultyId("amen");
        ReflectionTestUtils.setField(service, "openaiApiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "gpt-test");
        ReflectionTestUtils.setField(service, "maxCompletionTokens", 100);
        ReflectionTestUtils.setField(service, "requestTimeoutSeconds", 30);
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "systemPromptResource",
                new ByteArrayResource("Faculty={{APPROVED_FACULTY_CONTENT_JSON}} State={{USER_STATE_JSON}}".getBytes()));
    }

    @Test
    void sendsGroundedConversationAndPersistsBothTurns() throws Exception {
        FacultyContent faculty = new FacultyContent();
        faculty.setId("amen");
        faculty.setContentJson("{\"teaching\":\"stillness\"}");
        PracticeState state = new PracticeState();
        state.setFacultyId("amen");
        state.setCompletedDays(4);
        state.setLastPracticedAt(Instant.parse("2026-07-18T12:00:00Z"));
        state.setJournalAnalysisOptIn(true);
        ChatMessage old = new ChatMessage();
        old.setRole("assistant");
        old.setContent("Earlier reply");
        when(faculties.findById("amen")).thenReturn(Optional.of(faculty));
        when(states.findByUserIdAndFacultyId(user.getId(), "amen")).thenReturn(Optional.of(state));
        when(messages.findTop20ByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(old));
        mockResponse(200, "{\"choices\":[{\"message\":{\"content\":\"Be still.\"}}]}");

        assertThat(service.reply(user, "How should I practice?")).isEqualTo("Be still.");
        var captor = org.mockito.ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(ChatMessage::getRole).containsExactly("user", "assistant");
        assertThat(captor.getAllValues()).extracting(ChatMessage::getContent)
                .containsExactly("How should I practice?", "Be still.");
        assertThat(captor.getAllValues()).allSatisfy(message -> {
            assertThat(message.getUserId()).isEqualTo(user.getId());
            assertThat(message.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void createsMissingPracticeStateAndRepresentsNeverPracticed() throws Exception {
        FacultyContent faculty = new FacultyContent();
        faculty.setContentJson("{}");
        when(faculties.findById("amen")).thenReturn(Optional.of(faculty));
        when(states.findByUserIdAndFacultyId(user.getId(), "amen")).thenReturn(Optional.empty());
        when(states.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messages.findTop20ByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());
        mockResponse(200, "{\"choices\":[{\"message\":{\"content\":\"Welcome.\"}}]}");

        assertThat(service.reply(user, "Hello")).isEqualTo("Welcome.");
        verify(states).save(argThat(state -> state.getUserId().equals(user.getId())
                && state.getFacultyId().equals("amen")));
    }

    @Test
    void failsClearlyWhenFacultyWasNotSeeded() {
        when(faculties.findById("amen")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reply(user, "Hello"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("not seeded");
    }

    @Test
    void doesNotPersistWhenOpenAiRejectsRequest() throws Exception {
        FacultyContent faculty = new FacultyContent();
        faculty.setContentJson("{}");
        PracticeState state = new PracticeState();
        state.setFacultyId("amen");
        when(faculties.findById("amen")).thenReturn(Optional.of(faculty));
        when(states.findByUserIdAndFacultyId(user.getId(), "amen")).thenReturn(Optional.of(state));
        when(messages.findTop20ByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());
        mockResponse(429, "rate limited");

        assertThatThrownBy(() -> service.reply(user, "Hello"))
                .isInstanceOf(IOException.class).hasMessageContaining("429: rate limited");
        verify(messages, never()).save(any());
    }

    @Test
    void appliesConfiguredRequestTimeoutToOpenAiHttpRequest() throws Exception {
        ReflectionTestUtils.setField(service, "requestTimeoutSeconds", 7);
        stubHappyPathData();
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"Timed.\"}}]}");
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            assertThat(request.timeout()).hasValue(Duration.ofSeconds(7));
            return response;
        });

        assertThat(service.reply(user, "Hello")).isEqualTo("Timed.");
    }

    @Test
    void retriesOnceOnRateLimitThenSucceeds() throws Exception {
        stubHappyPathData();
        HttpResponse<String> first = mock(HttpResponse.class);
        HttpResponse<String> second = mock(HttpResponse.class);
        when(first.statusCode()).thenReturn(429);
        when(first.body()).thenReturn("rate limited");
        when(second.statusCode()).thenReturn(200);
        when(second.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"Recovered.\"}}]}");
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(first, second);

        assertThat(service.reply(user, "Hello")).isEqualTo("Recovered.");
        verify(client, times(2)).send(any(), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void doesNotRetryOnNonRetryable400Response() throws Exception {
        stubHappyPathData();
        mockResponse(400, "bad request");

        assertThatThrownBy(() -> service.reply(user, "Hello"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("400: bad request");
        verify(client, times(1)).send(any(), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void surfacesIoExceptionAfterExhaustingRetriesOnRepeatedServerErrors() throws Exception {
        stubHappyPathData();
        HttpResponse<String> first = mock(HttpResponse.class);
        HttpResponse<String> second = mock(HttpResponse.class);
        HttpResponse<String> third = mock(HttpResponse.class);
        when(first.statusCode()).thenReturn(500);
        when(first.body()).thenReturn("upstream error");
        when(second.statusCode()).thenReturn(500);
        when(second.body()).thenReturn("upstream error");
        when(third.statusCode()).thenReturn(500);
        when(third.body()).thenReturn("upstream error");
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(first, second, third);

        assertThatThrownBy(() -> service.reply(user, "Hello"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("500: upstream error");
        verify(client, times(3)).send(any(), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void doesNotRetryWhenRequestTimesOut() throws Exception {
        stubHappyPathData();
        when(client.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("request timed out"));

        assertThatThrownBy(() -> service.reply(user, "Hello"))
                .isInstanceOf(HttpTimeoutException.class)
                .hasMessageContaining("request timed out");
        verify(client, times(1)).send(any(), any(HttpResponse.BodyHandler.class));
    }

    private void stubHappyPathData() {
        FacultyContent faculty = new FacultyContent();
        faculty.setContentJson("{}");
        PracticeState state = new PracticeState();
        state.setFacultyId("amen");
        when(faculties.findById("amen")).thenReturn(Optional.of(faculty));
        when(states.findByUserIdAndFacultyId(user.getId(), "amen")).thenReturn(Optional.of(state));
        when(messages.findTop20ByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());
    }

    @SuppressWarnings("unchecked")
    private void mockResponse(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }
}
