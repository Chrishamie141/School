package com.notesapp.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.nio.file.Path;

public class PdfExporter {

    public static void export(String title, String note, String transcript, Path outFile) throws Exception {
        Document doc = new Document(PageSize.LETTER, 50, 50, 50, 50);
        PdfWriter.getInstance(doc, new FileOutputStream(outFile.toFile()));
        doc.open();

        // Title
        var titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        var hFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        var bodyFont = new Font(Font.HELVETICA, 11, Font.NORMAL);

        if (title == null || title.isBlank()) title = "Untitled Recording";
        doc.add(new Paragraph(title, titleFont));
        doc.add(new Paragraph(" ")); // spacer

        // Note
        doc.add(new Paragraph("Note", hFont));
        doc.add(new Paragraph(note == null ? "" : note, bodyFont));
        doc.add(new Paragraph(" "));

        // Transcript
        doc.add(new Paragraph("Transcript", hFont));
        doc.add(new Paragraph(transcript == null ? "" : transcript, bodyFont));

        doc.close();
    }
}
