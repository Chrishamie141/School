package com.notesapp.agents.dto;

import java.util.List;

public class TagNoteIn {
    private long noteId;
    private List<String> tags;

    public long getNoteId() { return noteId; }
    public void setNoteId(long noteId) { this.noteId = noteId; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
