package com.notesapp.export;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/**
 * PdfExporter Ã¢â‚¬â€œ Generates a combined PDF containing title, tag, notes, and transcript.
 */
public class PdfExporter {
    /**
     * Overload supporting tag field (Week 6+).
     */
    public static void export(String title, String note, String transcript, String tag, Path output) throws Exception {
        String finalTitle = (tag == null || tag.isBlank()) ? title : title + " [" + tag + "]";
        export(finalTitle, note, transcript, output);
    }
    /**
     * Original 4-argument export method (without tag).
     */
    public static void export(String title, String note, String transcript, Path output) throws Exception {
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, new FileOutputStream(output.toFile()));
        doc.open();
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        doc.add(new Paragraph(title, titleFont));
        doc.add(new Paragraph("Generated on: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), textFont));
        doc.add(Chunk.NEWLINE);
        // Notes section
        doc.add(new Paragraph("Notes", sectionFont));
        doc.add(new Paragraph(note == null ? "(none)" : note, textFont));
        doc.add(Chunk.NEWLINE);
        // Transcript section
        doc.add(new Paragraph("Transcript", sectionFont));
        doc.add(new Paragraph(transcript == null ? "(none)" : transcript, textFont));
        doc.close();
        System.out.println("PDF created: " + output.toAbsolutePath());
    }
}


