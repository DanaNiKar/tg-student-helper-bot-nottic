package model;

public class ScheduleEntry {
  public long id;
  public long chatId;
  public int dayOfWeek; // 1..7
  public String time; // "HH:mm"
  public String title;
  public String location;
  public boolean active;
}
