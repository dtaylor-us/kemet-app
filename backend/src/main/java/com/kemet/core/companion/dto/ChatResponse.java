package com.kemet.core.companion.dto;

public record ChatResponse(String reply, boolean aiGenerated) {
    public static ChatResponse of(String reply) {
        // aiGenerated is always true here — REQ-038/066: every companion response must
        // be labeled as AI-assisted by the client. Hardcoding true (rather than letting
        // it vary) is deliberate: there is no code path in this service that should ever
        // produce an unlabeled response.
        return new ChatResponse(reply, true);
    }
}
