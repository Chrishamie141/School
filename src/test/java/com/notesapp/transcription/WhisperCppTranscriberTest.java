package com.notesapp.transcription;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WhisperCppTranscriberTest {

    @Test
    void transcribeAudio_handlesMissingBinaryGracefully() throws Exception {
        TranscriptionService svc = new WhisperCppTranscriber();

        // Use a tiny temp file as a stand-in for audio; the impl should
        // throw a clear exception if whisper.cpp binary/model isn’t set.
        Path tmp = Files.createTempFile("fake-audio", ".wav");
        Files.writeString(tmp, "not real audio");

        try {
            String out = svc.transcribeAudio(tmp);
            // If whisper.cpp happens to be installed in your VM and the env is configured,
            // we just assert we got a non-null string back.
            assertNotNull(out);
        } catch (Exception e) {
            // Also acceptable: we just want a clear, descriptive error.
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            assertTrue(
                msg.contains("whisper") || msg.contains("model") || msg.contains("binary") || msg.contains("not configured"),
                "Expected a clear error about missing whisper.cpp setup, but got: " + e
            );
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
