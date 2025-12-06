package db;

import java.sql.*;

public class Database {
  private static final String DB_URL = "jdbc:sqlite:data.db";

  public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(DB_URL);
  }

  public static void init() {
    try (Connection c = getConnection();
         Statement s = c.createStatement()) {

      // tasks
      s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tasks (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  chat_id INTEGER,
                  title TEXT,
                  description TEXT,
                  deadline_ts INTEGER,
                  done INTEGER DEFAULT 0
                );
                """);

      // events
      s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS events (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  chat_id INTEGER,
                  title TEXT,
                  description TEXT,
                  event_ts INTEGER,
                  periodic INTEGER DEFAULT 0,
                  period_days INTEGER DEFAULT 0
                );
                """);

      // schedule
      s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS schedule (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  chat_id INTEGER,
                  day_of_week INTEGER,
                  time TEXT,
                  title TEXT,
                  location TEXT,
                  active INTEGER DEFAULT 1
                );
                """);

      System.out.println("Database initialized.");
    } catch (SQLException ex) {
      ex.printStackTrace();
      throw new RuntimeException(ex);
    }
  }
}
