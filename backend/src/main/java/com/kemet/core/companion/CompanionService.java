package com.kemet.core.companion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kemet.core.domain.AppUser;
import com.kemet.core.domain.ChatMessage;
import com.kemet.core.domain.FacultyContent;
import com.kemet.core.domain.PracticeState;
import com.kemet.core.repository.ChatMessageRepository;
import com.kemet.core.repository.FacultyContentRepository;
import com.kemet.core.repository.PracticeStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Builds the grounded system prompt (companion-system-prompt.md + seeded faculty
 * content + user state), calls the OpenAI Chat Completions API, and persists the turn.
 *
 * Switched from Anthropic to OpenAI on 2026-07-19 to draw down an existing OpenAI API
 * credit balance rather than incur new spend elsewhere — see the chat history for that
 * decision. The two APIs shape requests/responses differently (most notably: OpenAI
 * puts the system prompt as the first entry in the `messages` array rather than a
 * separate top-level field, and the response is `choices[0].message.content` rather
 * than a `content` array of typed blocks) — this class is a full rewrite of the request
 * plumbing, not a config change. The guardrail contract in companion-system-prompt.md
 * did not change; only how it gets delivered to the model did.
 *
 * This is a v0 implementation: no retry/backoff, no streaming, no structured
 * source-attribution field. See the "known gaps" section at the bottom of
 * companion-system-prompt.md before treating this as anything beyond a local prototype.
 */
@Service
public class CompanionService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final long OPENAI_RETRY_BASE_BACKOFF_MILLIS = 500L;

    private final ChatMessageRepository chatMessageRepository;
    private final PracticeStateRepository practiceStateRepository;
    private final FacultyContentRepository facultyContentRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.openai.api-key}")
    private String openaiApiKey;

    @Value("${app.openai.model}")
    private String model;

    @Value("${app.openai.max-completion-tokens}")
    private int maxCompletionTokens;

    @Value("${app.openai.request-timeout-seconds:30}")
    private int requestTimeoutSeconds;

    @Value("${app.openai.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.openai.system-prompt-path}")
    private Resource systemPromptResource;

    public CompanionService(ChatMessageRepository chatMessageRepository,
                             PracticeStateRepository practiceStateRepository,
                             FacultyContentRepository facultyContentRepository) {
        this(chatMessageRepository, practiceStateRepository, facultyContentRepository,
                new ObjectMapper(), HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build());
    }

    CompanionService(ChatMessageRepository chatMessageRepository,
                     PracticeStateRepository practiceStateRepository,
                     FacultyContentRepository facultyContentRepository,
                     ObjectMapper objectMapper,
                     HttpClient httpClient) {
        this.chatMessageRepository = chatMessageRepository;
        this.practiceStateRepository = practiceStateRepository;
        this.facultyContentRepository = facultyContentRepository;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public String reply(AppUser user, String userMessage) throws IOException, InterruptedException {
        String facultyId = user.getActiveFacultyId(); // all 11 faculties seeded now — see UserController for how this gets switched

        FacultyContent faculty = facultyContentRepository.findById(facultyId)
                .orElseThrow(() -> new IllegalStateException(
                        "Faculty content '" + facultyId + "' not seeded — did SeedDataLoader run?"));

        PracticeState state = practiceStateRepository
                .findByUserIdAndFacultyId(user.getId(), facultyId)
                .orElseGet(() -> {
                    PracticeState fresh = new PracticeState();
                    fresh.setUserId(user.getId());
                    fresh.setFacultyId(facultyId);
                    return practiceStateRepository.save(fresh);
                });

        String systemPrompt = buildSystemPrompt(faculty.getContentJson(), state);

        List<ChatMessage> history = chatMessageRepository
                .findTop20ByUserIdOrderByCreatedAtDesc(user.getId());
        Collections.reverse(history); // oldest first, as the API expects

        // OpenAI's Chat Completions API takes the system prompt as the first message in
        // the array (role "system"), not as a separate top-level field like Anthropic's
        // Messages API. Everything else about the array shape (role/content objects) is
        // the same idea.
        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(objectMapper.createObjectNode()
                .put("role", "system")
                .put("content", systemPrompt));
        for (ChatMessage m : history) {
            messages.add(objectMapper.createObjectNode()
                    .put("role", m.getRole())
                    .put("content", m.getContent()));
        }
        messages.add(objectMapper.createObjectNode()
                .put("role", "user")
                .put("content", userMessage));

        String assistantReply = callOpenAi(messages);

        persistTurn(user.getId(), "user", userMessage);
        persistTurn(user.getId(), "assistant", assistantReply);

        return assistantReply;
    }

    private String buildSystemPrompt(String facultyContentJson, PracticeState state) throws IOException {
        String template = new String(systemPromptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        ObjectNode userStateNode = objectMapper.createObjectNode();
        userStateNode.put("facultyId", state.getFacultyId());
        userStateNode.put("completedDays", state.getCompletedDays());
        userStateNode.put("lastPracticedAt",
                state.getLastPracticedAt() != null ? state.getLastPracticedAt().toString() : "never");
        userStateNode.put("journalAnalysisOptIn", state.isJournalAnalysisOptIn()); // REQ-052

        return template
                .replace("{{APPROVED_FACULTY_CONTENT_JSON}}", facultyContentJson)
                .replace("{{USER_STATE_JSON}}", userStateNode.toPrettyString());
    }

    private String callOpenAi(ArrayNode messages) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        // GPT-5-family models reject the older `max_tokens` field and require
        // `max_completion_tokens` instead — confirmed against current OpenAI docs as of
        // this rewrite (2026-07-19). If you're pointed at an older model that still
        // wants `max_tokens`, swap the field name back.
        body.put("max_completion_tokens", maxCompletionTokens);
        body.set("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Authorization", "Bearer " + openaiApiKey)
                .header("content-type", "application/json")
                .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        int totalAttempts = Math.max(1, maxAttempts);
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException ex) {
                throw ex; // timeout is already bounded by request timeout and should not be retried
            } catch (IOException ex) {
                if (attempt < totalAttempts) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                throw ex;
            }

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                // Chat Completions response shape: choices[0].message.content is a plain
                // string (unlike Anthropic's array-of-typed-blocks content field).
                return root.path("choices").path(0).path("message").path("content").asText();
            }

            if (isRetryableStatus(response.statusCode()) && attempt < totalAttempts) {
                sleepBeforeRetry(attempt);
                continue;
            }

            throw new IOException("OpenAI API returned " + response.statusCode() + ": " + response.body());
        }

        throw new IOException("OpenAI API request failed after " + totalAttempts + " attempts");
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private void sleepBeforeRetry(int attempt) throws InterruptedException {
        long backoffMillis = OPENAI_RETRY_BASE_BACKOFF_MILLIS * (1L << (attempt - 1));
        Thread.sleep(backoffMillis);
    }

    private void persistTurn(java.util.UUID userId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(Instant.now());
        chatMessageRepository.save(message);
    }
}
