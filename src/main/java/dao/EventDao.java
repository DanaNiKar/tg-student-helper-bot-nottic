package dao;

import db.Database;
import model.Event;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventDao {
  public static void add(Event e) throws SQLException {
    try (Connection c = Database.getConnection();
    PreparedStatement ps = c.prepareStatement(
        "INSERT INTO events(chat_id, title, description, event_ts, periodic, period_days) VALUES (?,?,?,?,?,?)",
        Statement.RETURN_GENERATED_KEYS)) {
      ps.setLong(1, e.chatId);
      ps.setString(2, e.title);
      ps.setString(3, e.description);
      ps.setLong(4, e.eventTs);
      ps.setInt(5, e.periodic ? 1 : 0);
      ps.setInt(6, e.periodDays);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) e.id = rs.getLong(1);
      }
    }
  }

  public static List<Event> listUpcoming(long chatId) throws SQLException {
    List<Event> res = new ArrayList<>();
    try (Connection c = Database.getConnection();
    PreparedStatement ps = c.prepareStatement("SELECT * FROM events WHERE chat_id = ? AND event_ts >= ? ORDER BY event_ts")) {
      ps.setLong(1, chatId);
      ps.setLong(2, System.currentTimeMillis() / 1000);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Event e = new Event();
          e.id = rs.getLong("id");
          e.chatId = rs.getLong("chat_id");
          e.title = rs.getString("title");
          e.description = rs.getString("description");
          e.eventTs = rs.getLong("event_ts");
          e.periodic = rs.getInt("periodic") == 1;
          e.periodDays = rs.getInt("period_days");
          res.add(e);
        }
      }
    }
    return res;
  }

  public static void update(Event e) throws SQLException {
    try (Connection c = Database.getConnection();
    PreparedStatement ps = c.prepareStatement("UPDATE events SET title=?, description=?, event_ts=?, periodic=?, period_days=? WHERE id=?")) {
      ps.setString(1, e.title);
      ps.setString(2, e.description);
      ps.setLong(3, e.eventTs);
      ps.setInt(4, e.periodic ? 1 : 0);
      ps.setInt(5, e.periodDays);
      ps.setLong(6, e.id);
      ps.executeUpdate();
    }
  }

  public static Event findById(long id) throws SQLException {
    try (Connection c = Database.getConnection();
    PreparedStatement ps = c.prepareStatement("SELECT * FROM events WHERE id = ?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          Event e = new Event();
          e.id = rs.getLong("id");
          e.chatId = rs.getLong("chat_id");
          e.title = rs.getString("title");
          e.description = rs.getString("description");
          e.eventTs = rs.getLong("event_ts");
          e.periodic = rs.getInt("periodic") == 1;
          e.periodDays = rs.getInt("period_days");
          return e;
        }
      }
    }
    return null;
  }
}
