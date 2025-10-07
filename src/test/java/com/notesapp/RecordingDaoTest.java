package com.notesapp;
import com.notesapp.db.Database; import com.notesapp.dao.*; import com.notesapp.model.*;
import org.junit.jupiter.api.*; import java.sql.*; import java.time.*;
public class RecordingDaoTest {
  private Connection conn; private RecordingDao dao;
  @BeforeEach void setup() throws Exception { conn=Database.get(); dao=new RecordingDao(conn); }
  @Test void insertAndFetch() throws Exception {
    Recording r=dao.insert(new Recording("Test","/tmp/a.wav",0,OffsetDateTime.now().toString()));
    Assertions.assertTrue(dao.findById(r.getId()).isPresent());
  }
}
