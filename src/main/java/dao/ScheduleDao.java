package dao;

import db.Database;
import model.ScheduleEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDao {
  public static void add(ScheduleEntry e) throws SQLException {
    try (Connection c = Database.getConnection();
         PreparedStatement ps = c.prepareStatement(
             "INSERT INTO schedule(chat_id, day_of_week, time, title, location, active) VALUES (?,?,?,?,?,?)",
             Statement.RETURN_GENERATED_KEYS)) {
      ps.setLong(1, e.chatId);
      ps.setInt(2, e.dayOfWeek);
      ps.setString(3, e.time);
      ps.setString(4, e.title);
      ps.setString(5, e.location);
      ps.setInt(6, e.active ? 1 : 0);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) e.id = rs.getLong(1);
      }
    }
  }

  public static List<ScheduleEntry> listForChat(long chatId) throws SQLException {
    List<ScheduleEntry> res = new ArrayList<>();
    try (Connection c = Database.getConnection();
         PreparedStatement ps = c.prepareStatement("SELECT * FROM schedule WHERE chat_id = ? ORDER BY day_of_week, time")) {
      ps.setLong(1, chatId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          ScheduleEntry e = new ScheduleEntry();
          e.id = rs.getLong("id");
          e.chatId = rs.getLong("chat_id");
          e.dayOfWeek = rs.getInt("day_of_week");
          e.time = rs.getString("time");
          e.title = rs.getString("title");
          e.location = rs.getString("location");
          e.active = rs.getInt("active") == 1;
          res.add(e);
        }
      }
    }
    return res;
  }

  public static void update(ScheduleEntry e) throws SQLException {
    try (Connection c = Database.getConnection();
         PreparedStatement ps = c.prepareStatement("UPDATE schedule SET day_of_week=?,time=?,title=?,location=?,active=? WHERE id=?")) {
      ps.setInt(1, e.dayOfWeek);
      ps.setString(2, e.time);
      ps.setString(3, e.title);
      ps.setString(4, e.location);
      ps.setInt(5, e.active ? 1 : 0);
      ps.setLong(6, e.id);
      ps.executeUpdate();
    }
  }
}
