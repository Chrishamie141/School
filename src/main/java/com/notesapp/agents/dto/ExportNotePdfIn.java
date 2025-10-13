package com.notesapp.agents.dto;

import java.nio.file.Path;

public class ExportNotePdfIn {
    private long noteId;
    private Path exportPath;

    public long getNoteId() { return noteId; }
    public void setNoteId(long noteId) { this.noteId = noteId; }

    public Path getExportPath() { return exportPath; }
    public void setExportPath(Path exportPath) { this.exportPath = exportPath; }
}
