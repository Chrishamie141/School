package com.notesapp.ui;














import com.notesapp.agents.dto.ExportNotePdfIn;
import com.notesapp.agents.dto.SearchNotesIn;
import com.notesapp.agents.dto.TagNoteIn;
import com.notesapp.agents.dto.SaveNoteIn;
import com.notesapp.agents.dto.TranscribeAudioIn;
import com.notesapp.agents.dto.ImportAudioIn;
import com.notesapp.agents.impl.ExportNotePdfAgent;
import com.notesapp.agents.impl.SearchNotesAgent;
import com.notesapp.agents.impl.TagNoteAgent;
import com.notesapp.agents.impl.SaveNoteAgent;
import com.notesapp.agents.impl.TranscribeAudioAgent;
import com.notesapp.agents.impl.ImportAudioAgent;
import java.nio.file.Path;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.control.Button;

public class MainController {
    // ===== Agent instances (stubbed implementations) =====
    private final ImportAudioAgent importAudioAgent = new ImportAudioAgent();
    private final TranscribeAudioAgent transcribeAudioAgent = new TranscribeAudioAgent();
    private final SaveNoteAgent saveNoteAgent = new SaveNoteAgent();
    private final TagNoteAgent tagNoteAgent = new TagNoteAgent();
    private final SearchNotesAgent searchNotesAgent = new SearchNotesAgent();
    private final ExportNotePdfAgent exportNotePdfAgent = new ExportNotePdfAgent();


    @FXML private TextField searchField;
    @FXML private TextField tagField;
    @FXML private TextArea noteArea;
    @FXML private TextArea transcriptArea;
    @FXML private ListView<String> recordingList;
    @FXML private Label statusLabel;

    // ===== Event Handlers =====

    @FXML
    private void onSearch(ActionEvent event) {
    try {
        statusLabel.setText("🔍 Searching...");
        searchNotesAgent.run(new SearchNotesIn()); // using stub DTO
        statusLabel.setText("✅ Search complete.");
    } catch (Exception e) {
        statusLabel.setText("⚠️ Search failed: " + e.getMessage());
        e.printStackTrace();
    }
}

    @FXML
    private void onNewRecording(ActionEvent event) {
    try {
        statusLabel.setText("🎙️ Importing audio...");
        importAudioAgent.run(new ImportAudioIn()); // using stub DTO
        statusLabel.setText("✅ Audio imported.");
    } catch (Exception e) {
        statusLabel.setText("⚠️ Import failed: " + e.getMessage());
        e.printStackTrace();
    }
}

    @FXML
    private void onTranscribeAndSave(ActionEvent event) {
    try {
        statusLabel.setText("🧠 Transcribing...");
        transcribeAudioAgent.run(new TranscribeAudioIn()); // using stub DTO
        statusLabel.setText("✅ Transcription complete.");
    } catch (Exception e) {
        statusLabel.setText("⚠️ Transcription failed: " + e.getMessage());
        e.printStackTrace();
    }
}

    @FXML
    private void onSave(ActionEvent event) {
    try {
        statusLabel.setText("💾 Saving note...");
        saveNoteAgent.run(new SaveNoteIn()); // using stub DTO
        statusLabel.setText("✅ Note saved.");
    } catch (Exception e) {
        statusLabel.setText("⚠️ Save failed: " + e.getMessage());
        e.printStackTrace();
    }
}

    @FXML
    private void onBackToHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setTitle("School Notes App");
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error returning to home: " + e.getMessage());
        }
    }
}



