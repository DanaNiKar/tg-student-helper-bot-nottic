package model;

public class Event {
  public long id;
  public long chatId;
  public String title;
  public String description;
  public long eventTs; //epoch seconds
  public boolean periodic;
  public int periodDays; // если periodic, период в днях
  public Event() {}
}
