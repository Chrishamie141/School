package com.notesapp.agents;

import com.notesapp.agents.dto.*;
import com.notesapp.agents.impl.*;
import java.nio.file.Path;
import java.util.List;

public class AgentRunner {

    public static void main(String[] args) {
        System.out.println("🤖 Running School Notes Agent System...\n");

        try {
            // ===== 1️⃣ Import Audio =====
            ImportAudioAgent importAgent = new ImportAudioAgent();
            ImportAudioIn importIn = new ImportAudioIn();
            importIn.setSource(Path.of("C:/mock/audio/source.wav"));
            importIn.setDest(Path.of("C:/mock/audio/dest.wav"));
            importAgent.run(importIn);
            System.out.println("✅ ImportAudioAgent executed.\n");

            // ===== 2️⃣ Transcribe Audio =====
            TranscribeAudioAgent transcribeAgent = new TranscribeAudioAgent();
            TranscribeAudioIn transcribeIn = new TranscribeAudioIn();
            transcribeIn.setAudioPath(Path.of("C:/mock/audio/dest.wav"));
            transcribeAgent.run(transcribeIn);
            System.out.println("✅ TranscribeAudioAgent executed.\n");

            // ===== 3️⃣ Save Note =====
            SaveNoteAgent saveAgent = new SaveNoteAgent();
            SaveNoteIn saveIn = new SaveNoteIn();
            saveIn.setTitle("Lecture Notes - Week 5");
            saveIn.setContent("These are sample lecture notes saved via the agent system.");
            saveAgent.run(saveIn);
            System.out.println("✅ SaveNoteAgent executed.\n");

            // ===== 4️⃣ Tag Note =====
            TagNoteAgent tagAgent = new TagNoteAgent();
            TagNoteIn tagIn = new TagNoteIn();
            tagIn.setNoteId(1);
            tagIn.setTags(List.of("JavaFX", "OOP", "SchoolNotes"));
            tagAgent.run(tagIn);
            System.out.println("✅ TagNoteAgent executed.\n");

            // ===== 5️⃣ Search Notes =====
            SearchNotesAgent searchAgent = new SearchNotesAgent();
            SearchNotesIn searchIn = new SearchNotesIn();
            searchIn.setKeyword("Lecture");
            searchAgent.run(searchIn);
            System.out.println("✅ SearchNotesAgent executed.\n");

            // ===== 6️⃣ Export PDF =====
            ExportNotePdfAgent exportAgent = new ExportNotePdfAgent();
            ExportNotePdfIn exportIn = new ExportNotePdfIn();
            exportIn.setNoteId(1);
            exportIn.setExportPath(Path.of("C:/mock/exports/lecture5.pdf"));
            exportAgent.run(exportIn);
            System.out.println("✅ ExportNotePdfAgent executed.\n");

            System.out.println("🎯 All agents executed successfully.");
        } catch (Exception e) {
            System.err.println("🔥 AgentRunner failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
