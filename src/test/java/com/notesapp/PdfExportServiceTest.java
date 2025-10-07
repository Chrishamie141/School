package com.notesapp;
import com.notesapp.service.PdfExportService;
import org.junit.jupiter.api.*;
import java.nio.file.*;

public class PdfExportServiceTest {
  @Test void exportCreatesFile() throws Exception {
    PdfExportService svc = new PdfExportService();
    Path p = svc.exportSideBySide("JUnit Sample","CPS-101","2025-09-01T12:00:00Z","Transcript text","Notes text");
    Assertions.assertTrue(Files.exists(p));
    // Cleanup optional:
    // Files.deleteIfExists(p);
  }
}
