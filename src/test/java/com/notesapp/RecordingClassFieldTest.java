package com.notesapp;
import com.notesapp.db.Database;
import com.notesapp.dao.RecordingDao;
import com.notesapp.model.Recording;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.time.OffsetDateTime;

public class RecordingClassFieldTest {
  private Connection conn; private RecordingDao dao;

  @BeforeEach void setup() throws Exception { conn = Database.get(); dao = new RecordingDao(conn); }

  @Test void updateAndReadClassName() throws Exception {
    Recording r = dao.insert(new Recording("Chem Lecture","/tmp/chem.wav",0,OffsetDateTime.now().toString()));
    dao.updateClassName(r.getId(), "CHEM-201");
    Recording got = dao.findById(r.getId()).orElseThrow();
    Assertions.assertEquals("CHEM-201", got.getClassName());
  }
}
