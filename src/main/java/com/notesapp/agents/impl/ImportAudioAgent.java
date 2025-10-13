package com.notesapp.agents.impl;

import com.notesapp.agents.dto.ImportAudioIn;
import java.nio.file.Path;
import java.util.Optional;

public class ImportAudioAgent {
    public Optional<ImportAudioIn> run(ImportAudioIn input) {
        System.out.println("🎵 Importing audio...");
        try {
            input.setDest(Path.of("data/recordings/lecture1.mp3"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.of(input);
    }
}
