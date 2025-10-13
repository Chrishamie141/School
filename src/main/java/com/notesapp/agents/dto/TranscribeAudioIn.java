package com.notesapp.agents.dto;

import java.nio.file.Path;

public class TranscribeAudioIn {
    private Path audioPath;

    public Path getAudioPath() { return audioPath; }
    public void setAudioPath(Path audioPath) { this.audioPath = audioPath; }
}
