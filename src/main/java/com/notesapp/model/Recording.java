package com.notesapp.model;
public class Recording {
  private int id;
  private String title;
  private String path;
  private int durationSec;
  private String recordedAt;
  private String className;
  public Recording() {}
  public Recording(int id, String title, String path, int durationSec, String recordedAt, String className) {
    this.id = id; this.title = title; this.path = path; this.durationSec = durationSec; this.recordedAt = recordedAt; this.className = className;
  }
  public Recording(String title, String path, int durationSec, String recordedAt) {
    this(0, title, path, durationSec, recordedAt, null);
  }
  public int getId() { return id; }
  public void setId(int id) { this.id = id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getPath() { return path; }
  public void setPath(String path) { this.path = path; }
  public int getDurationSec() { return durationSec; }
  public void setDurationSec(int durationSec) { this.durationSec = durationSec; }
  public String getRecordedAt() { return recordedAt; }
  public void setRecordedAt(String recordedAt) { this.recordedAt = recordedAt; }
  public String getClassName() { return className; }
  public void setClassName(String className) { this.className = className; }
  @Override public String toString() { return title; }
}


