package com.notesapp.agents.impl;

import com.notesapp.agents.dto.TranscribeAudioIn;
import com.notesapp.agents.dto.TranscribeAudioOut;
import java.util.Optional;

public class TranscribeAudioAgent {
    public Optional<TranscribeAudioOut> run(TranscribeAudioIn input) {
        System.out.println("🗣 Transcribing audio...");
        TranscribeAudioOut out = new TranscribeAudioOut();
        try {
            out.setTranscriptId("mock-transcript-id");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.of(out);
    }
}
