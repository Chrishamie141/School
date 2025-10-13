package com.notesapp.agents.dto;

import java.util.List;
import java.util.ArrayList;

public class SearchNotesOut {
    private List<Object> results = new ArrayList<>();

    public List<Object> getResults() { return results; }
    public void setResults(List<Object> results) { this.results = results; }
}
