package com.notesapp.model;
public class Transcript {
  private int id; private int recordingId; private String content; private String createdAt;
  public Transcript() {}
  public Transcript(int id,int recordingId,String content,String createdAt){this.id=id;this.recordingId=recordingId;this.content=content;this.createdAt=createdAt;}
  public Transcript(int recordingId,String content,String createdAt){this(0,recordingId,content,createdAt);}
  public int getId(){return id;} public void setId(int id){this.id=id;}
  public int getRecordingId(){return recordingId;} public void setRecordingId(int r){this.recordingId=r;}
  public String getContent(){return content;} public void setContent(String c){this.content=c;}
  public String getCreatedAt(){return createdAt;} public void setCreatedAt(String c){this.createdAt=c;}
}
