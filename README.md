HEAD
# School
calculator app

# School Notes App

A desktop JavaFX app for taking notes from class recordings and exporting to PDF.

## Week One Deliverables
- ✅ JavaFX entrypoint (`com.notesapp.MainFX`) + sidebar
- ✅ SQLite DB init + NoteDao
- ✅ Transcription stub
- ✅ PDF export (OpenPDF)
- ✅ Fat JAR headless run in Codespaces
- ✅ JUnit 5 test

## Requirements
- Java 21+
- Maven or the included Maven Wrapper

## Run in Codespaces
mvn -q -DskipTests clean package
java -jar target/school-notes-app-1.0-SNAPSHOT-jar-with-dependencies.jar

## Run Locally (GUI)
mvn javafx:run
ea2f686 (Week 3 complete: added DAO integration tests + export + fixed Path handling)
