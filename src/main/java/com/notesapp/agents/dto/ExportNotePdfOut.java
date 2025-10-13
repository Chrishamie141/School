package com.notesapp.agents.dto;

import java.nio.file.Path;

public class ExportNotePdfOut {
    private Path pdfPath;

    public Path getPdfPath() { return pdfPath; }
    public void setPdfPath(Path pdfPath) { this.pdfPath = pdfPath; }
}
