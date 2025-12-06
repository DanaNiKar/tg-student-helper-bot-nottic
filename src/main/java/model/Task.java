package model;

public class Task {
  public long id;
  public long chatId;
  public String title;
  public String description;
  public long deadlineTs; // epoch seconds
  public boolean done;

  public Task() {}
}
