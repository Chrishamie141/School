package com.notesapp.agents.impl;

import com.notesapp.agents.dto.TagNoteIn;
import java.util.Optional;

public class TagNoteAgent {
    public Optional<String> run(TagNoteIn input) {
        System.out.println("?? Tagging note...");
        return Optional.of("ok");
    }
}
