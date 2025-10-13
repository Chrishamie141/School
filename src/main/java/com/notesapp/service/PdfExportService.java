package com.notesapp.service;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.*;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
public class PdfExportService {
  public Path exportSideBySide(String title,
                               String className,
                               String recordedAt,
                               String transcript,
                               String notes) throws Exception {
    Path outDir = Paths.get("data", "exports");
    Files.createDirectories(outDir);
    String safeTitle = (title == null || title.isBlank()) ? "untitled"
        : title.replaceAll("[^a-zA-Z0-9-_ ]","").trim();
    String ts = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    Path out = outDir.resolve(ts + "_" + safeTitle + ".pdf");
    Document doc = new Document(PageSize.LETTER.rotate(), 36, 36, 36, 36);
    try (OutputStream os = Files.newOutputStream(out)) {
      PdfWriter writer = PdfWriter.getInstance(doc, os);
      // prevent writer from closing stream before try-with-resources exits
      writer.setCloseStream(false);
      doc.addTitle("School Notes Export");
      doc.addAuthor("School Notes App");
      doc.open();
      // Header
      Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
      Font meta = FontFactory.getFont(FontFactory.HELVETICA, 10);
      Paragraph pTitle = new Paragraph("Notes Export Ã¢â‚¬â€ " + safeTitle, h1);
      pTitle.setSpacingAfter(6f);
      doc.add(pTitle);
      doc.add(new Paragraph("Class: " + (className==null?"Ã¢â‚¬â€":className), meta));
      doc.add(new Paragraph("Recorded: " + (recordedAt==null?"Ã¢â‚¬â€":recordedAt), meta));
      doc.add(new Paragraph("Exported: " + OffsetDateTime.now().toString(), meta));
      doc.add(Chunk.NEWLINE);
      // Two-column table
      PdfPTable table = new PdfPTable(new float[]{1f,1f});
      table.setWidthPercentage(100f);
      PdfPCell leftHeader = new PdfPCell(new Phrase("Transcript",
          FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
      PdfPCell rightHeader = new PdfPCell(new Phrase("Notes",
          FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
      leftHeader.setGrayFill(0.92f); rightHeader.setGrayFill(0.92f);
      leftHeader.setPadding(6f); rightHeader.setPadding(6f);
      table.addCell(leftHeader); table.addCell(rightHeader);
      PdfPCell leftBody = new PdfPCell(new Phrase(nullToDash(transcript),
          FontFactory.getFont(FontFactory.HELVETICA, 11)));
      PdfPCell rightBody = new PdfPCell(new Phrase(nullToDash(notes),
          FontFactory.getFont(FontFactory.HELVETICA, 11)));
      leftBody.setPadding(8f); rightBody.setPadding(8f);
      leftBody.setVerticalAlignment(Element.ALIGN_TOP);
      rightBody.setVerticalAlignment(Element.ALIGN_TOP);
      table.addCell(leftBody); table.addCell(rightBody);
      doc.add(table);
      // Close document before OutputStream auto-closes
      doc.close();
    }
    return out;
  }
  private static String nullToDash(String s){ return (s==null || s.isBlank()) ? "Ã¢â‚¬â€" : s; }
}


