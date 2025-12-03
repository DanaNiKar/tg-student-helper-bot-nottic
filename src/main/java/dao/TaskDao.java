package dao;

import db.Database;
import model.Task;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDao {
  public static void add(Task t) throws SQLException {
    try (Connection c = Database.getConnection();
    PreparedStatement ps = c.prepareStatement(
        "INSERT INTO tasks(chat_id, title, description, deadline_ts, done) VALUES (?,?,?,?,?)",
        Statement.RETURN_GENERATED_KEYS)) {
      ps.setLong(1, t.chatId);
      ps.setString(2, t.title);
      ps.setString(3, t.description);
      ps.setLong(4, t.deadlineTs);
      ps.setInt(5, t.done ? 1 : 0);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) t.id = rs.getLong(1);
      }
    }
  }

  public static List<Task> list(long chatId) throws SQLException {
    List<Task> res = new ArrayList<>();
    try (Connection c = Database.getConnection();
    PreparedStatement ps = c.prepareStatement("SELECT * FROM tasks WHERE chat_id = ?")) {
      ps.setLong(1, chatId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Task t = new Task();
          t.id = rs.getLong("id");
          t.chatId = rs.getLong("chat_id");
          t.title = rs.getString("title");
          t.description = rs.getString("description");
          t.deadlineTs = rs.getLong("deadline_ts");
          t.done = rs.getInt("done") == 1;
          res.add(t);
        }
      }
    }
    return res;
  }

  public static void markDone(long id, boolean done) throws SQLException {
    try (Connection c = Database.getConnection();
    PreparedStatement ps = c.prepareStatement("UPDATE tasks SET done = ? WHEER id = ?")) {
      ps.setInt(1, done ? 1 : 0);
      ps.setLong(2, id);
      ps.executeUpdate();
    }
  }

  public static Task findById(long id) throws SQLException {
    try (Connection c = Database.getConnection();
    PreparedStatement ps = c.prepareStatement("SELECT * FROM tasks WHERE id = ?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          Task t = new Task();
          t.id = rs.getLong("id");
          t.chatId = rs.getLong("chat_id");
          t.title = rs.getString("title");
          t.description = rs.getString("description");
          t.deadlineTs = rs.getLong("deadline_ts");
          t.done = rs.getInt("done") == 1;
          return t;
        }
      }
    }
    return null;
  }
}
