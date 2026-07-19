package com.kemet.core.companion;

import com.kemet.core.companion.dto.ChatResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ChatResponseAdl026Test {

    @ParameterizedTest(name = "ADL-026: ChatResponse.of should mark aiGenerated=true for input: ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {"hello", "   ", "𓂀"})
    void adl026_chatResponseOfAlwaysMarksAiGeneratedTrue(String reply) {
        ChatResponse response = ChatResponse.of(reply);

        assertThat(response.aiGenerated())
                .withFailMessage("ADL-026 violated: ChatResponse.of(%s) must always set aiGenerated=true", reply)
                .isTrue();
    }
}
