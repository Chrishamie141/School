package com.notesapp.agents.impl;

import com.notesapp.agents.dto.SearchNotesIn;
import com.notesapp.agents.dto.SearchNotesOut;
import java.util.Optional;
import java.util.List;

public class SearchNotesAgent {
    public Optional<SearchNotesOut> run(SearchNotesIn input) {
        System.out.println("🔍 Searching notes...");
        SearchNotesOut out = new SearchNotesOut();
        try {
            out.setResults(List.of());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.of(out);
    }
}
