package com.notesapp.agents.impl;

import com.notesapp.agents.dto.ExportNotePdfIn;
import com.notesapp.agents.dto.ExportNotePdfOut;
import java.nio.file.Path;
import java.util.Optional;

public class ExportNotePdfAgent {
    public Optional<ExportNotePdfOut> run(ExportNotePdfIn input) {
        System.out.println("📤 Exporting note to PDF...");
        ExportNotePdfOut out = new ExportNotePdfOut();
        try {
            out.setPdfPath(Path.of("exports/mock.pdf"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.of(out);
    }
}
