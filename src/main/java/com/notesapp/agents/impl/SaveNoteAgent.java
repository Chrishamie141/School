package com.notesapp.agents.impl;

import com.notesapp.agents.dto.SaveNoteIn;
import com.notesapp.agents.dto.SaveNoteOut;
import java.util.Optional;

public class SaveNoteAgent {
    public Optional<SaveNoteOut> run(SaveNoteIn input) {
        System.out.println("📝 Saving note...");
        SaveNoteOut out = new SaveNoteOut();
        try {
            out.setNoteId(1001L);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.of(out);
    }
}
