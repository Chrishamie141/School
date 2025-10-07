package com.notesapp.ui;

import com.notesapp.dao.NoteDao;
import com.notesapp.dao.RecordingDao;
import com.notesapp.dao.TranscriptDao;
import com.notesapp.db.DatabaseManager;
import com.notesapp.transcription.TranscriptionService;
import com.notesapp.transcription.WhisperCppTranscriber;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.io.File;
import java.sql.Connection;
import java.util.List;

public class MainController {

    @FXML private TextArea noteArea;
    @FXML private TextArea transcriptArea;
    @FXML private Label statusLabel;
    @FXML private TextField searchField;
    @FXML private TextField tagField;
    @FXML private ListView<String> recordingList;

    private Connection conn;
    private RecordingDao recordingDao;
    private NoteDao noteDao;
    private TranscriptDao transcriptDao;

    private final TranscriptionService transcriber = new WhisperCppTranscriber();

    @FXML
    public void initialize() {
        try {
            conn = DatabaseManager.getConnection(); // unified connection
            recordingDao = new RecordingDao(conn);
            noteDao = new NoteDao(conn);
            transcriptDao = new TranscriptDao(conn);

            configureListView();
            refreshRecordingList();

            statusLabel.setText("✅ Connected to database");
        } catch (Exception e) {
            statusLabel.setText("❌ DB connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* ---------------------- ListView Enhancements ---------------------- */

    private void configureListView() {
        recordingList.setCellFactory(new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> listView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(String title, boolean empty) {
                        super.updateItem(title, empty);
                        if (empty || title == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(title);
                            try {
                                String tag = recordingDao.getTagForRecording(title);
                                if (tag != null && !tag.isBlank()) {
                                    if (tag.equalsIgnoreCase("important"))
                                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                                    else if (tag.equalsIgnoreCase("lecture"))
                                        setStyle("-fx-text-fill: #1E88E5;");
                                    else if (tag.equalsIgnoreCase("exam"))
                                        setStyle("-fx-text-fill: #9C27B0;");
                                    else
                                        setStyle("-fx-text-fill: #388E3C;");
                                } else {
                                    setStyle("-fx-text-fill: black;");
                                }
                            } catch (Exception ignored) {
                                setStyle("-fx-text-fill: black;");
                            }
                        }
                    }
                };
            }
        });
    }

    /* ---------------------- UI Actions ---------------------- */

    @FXML
    private void onSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        try {
            List<String> results = keyword.isEmpty()
                    ? recordingDao.getAllRecordingNames()
                    : recordingDao.searchByKeyword(keyword);
            recordingList.getItems().setAll(results);
            statusLabel.setText(results.size() + " result(s)");
        } catch (Exception e) {
            statusLabel.setText("❌ Search failed: " + e.getMessage());
        }
    }

    @FXML
    private void onRecordingSelect() {
        String title = recordingList.getSelectionModel().getSelectedItem();
        if (title == null) return;
        try {
            tagField.setText(recordingDao.getTagForRecording(title));
            noteArea.setText(noteDao.findByTitle(title).orElse(""));
            transcriptArea.setText(transcriptDao.findByTitle(title).orElse(""));
            statusLabel.setText("📂 Loaded: " + title);
        } catch (Exception e) {
            statusLabel.setText("❌ Load failed: " + e.getMessage());
        }
    }

    @FXML
    private void onSave(ActionEvent event) {
        String title = recordingList.getSelectionModel().getSelectedItem();
        if (title == null) {
            statusLabel.setText("⚠️ No recording selected");
            return;
        }
        try {
            String tag = tagField.getText().trim();
            recordingDao.setTagForRecording(title, tag);
            noteDao.save(title, noteArea.getText());
            transcriptDao.save(title, transcriptArea.getText());
            statusLabel.setText("✅ Saved successfully");

            Platform.runLater(() -> {
                try { refreshRecordingList(); } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            statusLabel.setText("❌ Save failed: " + e.getMessage());
        }
    }

    @FXML
    private void onNewRecording(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.m4a"));
        File file = fc.showOpenDialog(null);
        if (file == null) return;

        try {
            String title = file.getName();
            long id = recordingDao.insert(title, file.getAbsolutePath(), 0L, System.currentTimeMillis());
            refreshRecordingList();
            statusLabel.setText("🎧 Added new recording: " + title + " (ID " + id + ")");
        } catch (Exception e) {
            statusLabel.setText("❌ Add failed: " + e.getMessage());
        }
    }

    /* ---------------------- 🎙️ Transcribe & Save ---------------------- */

    @FXML
    private void onTranscribeAndSave(ActionEvent event) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.m4a"));
            File file = chooser.showOpenDialog(null);
            if (file == null) {
                statusLabel.setText("⚠️ No file selected");
                return;
            }

            statusLabel.setText("🎙️ Transcribing " + file.getName() + "...");
            // FIX: use transcribeAudio() instead of transcribe()
            String transcriptText = transcriber.transcribeAudio(file.toPath());

            if (transcriptText == null || transcriptText.isBlank()) {
                statusLabel.setText("⚠️ No transcript text detected");
                return;
            }

            long recordingId = recordingDao.insert(
                    file.getName(),
                    file.getAbsolutePath(),
                    0L,
                    System.currentTimeMillis()
            );

            transcriptDao.upsertByRecordingId(recordingId, transcriptText);
            transcriptArea.setText(transcriptText);
            statusLabel.setText("✅ Transcription saved for " + file.getName());

            refreshRecordingList();

        } catch (Exception e) {
            statusLabel.setText("❌ Transcription failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* ---------------------- Helpers ---------------------- */

    private void refreshRecordingList() {
        try {
            List<String> all = recordingDao.getAllRecordingNames();
            recordingList.getItems().setAll(all);
            recordingList.refresh();
        } catch (Exception e) {
            statusLabel.setText("❌ Refresh failed: " + e.getMessage());
        }
    }

    public void close() {
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
    }
}
