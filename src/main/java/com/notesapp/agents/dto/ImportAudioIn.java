package com.notesapp.agents.dto;

import java.nio.file.Path;

public class ImportAudioIn {
    private Path source;
    private Path dest;

    public Path getSource() { return source; }
    public void setSource(Path source) { this.source = source; }

    public Path getDest() { return dest; }
    public void setDest(Path dest) { this.dest = dest; }
}
