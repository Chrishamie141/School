package com.notesapp.model;
public class Note {
  private int id; private int recordingId; private String content; private String updatedAt;
  public Note() {}
  public Note(int id,int recordingId,String content,String updatedAt){this.id=id;this.recordingId=recordingId;this.content=content;this.updatedAt=updatedAt;}
  public Note(int recordingId,String content,String updatedAt){this(0,recordingId,content,updatedAt);}
  public int getId(){return id;} public void setId(int id){this.id=id;}
  public int getRecordingId(){return recordingId;} public void setRecordingId(int r){this.recordingId=r;}
  public String getContent(){return content;} public void setContent(String c){this.content=c;}
  public String getUpdatedAt(){return updatedAt;} public void setUpdatedAt(String u){this.updatedAt=u;}
}
