package service;

import dao.EventDao;
import model.Event;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import bot.StudentHelperBot;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class ReminderService {
  private static ReminderService instance;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
  private final Map<Long, List<ScheduledFuture<?>>> scheduledMap = new ConcurrentHashMap<>();
  private StudentHelperBot bot;

  private ReminderService() {}

  public static ReminderService getInstance() {
    if (instance == null) instance = new ReminderService();
    return instance;
  }

  public void setBot(StudentHelperBot bot) {
    this.bot = bot;
  }

  public void reloadAllReminders() {
    try {
      List<Event> all = new ArrayList<>();
      try (var conn = db.Database.getConnection();
           var ps = conn.prepareStatement("SELECT * FROM events WHERE event_ts >= ?")) {
        ps.setLong(1, Instant.now().getEpochSecond());
        try (var rs = ps.executeQuery()) {
          while (rs.next()) {
            Event e = new Event();
            e.id = rs.getLong("id");
            e.chatId = rs.getLong("chat_id");
            e.title = rs.getString("title");
            e.description = rs.getString("description");
            e.eventTs = rs.getLong("event_ts");
            e.periodic = rs.getInt("periodic") == 1;
            e.periodDays = rs.getInt("period_days");
            all.add(e);
          }
        }
      }
      for (Event e : all) scheduleRemindersForEvent(e);
      System.out.println("Reminders reloaded: " + all.size());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void scheduleRemindersForEvent(Event e) {
    cancelScheduled(e.id);

    long now = Instant.now().getEpochSecond();
    long eventTs = e.eventTs;
    if (eventTs <= now) {
      if (e.periodic && e.periodDays > 0) {
        long next = eventTs + e.periodDays * 24L * 3600L;
        e.eventTs = next;
        try {
          EventDao.update(e);
        } catch (Exception ex) { ex.printStackTrace(); }
        eventTs = next;
      } else {
        return;
      }
    }

    List<Long> offsets = new ArrayList<>();
    offsets.add(7 * 24L * 3600L);
    offsets.add(24L * 3600L);
    offsets.add(3600L);
    offsets.add(600L);

    List<ScheduledFuture<?>> futures = new ArrayList<>();
    for (Long offset : offsets) {
      long remindAt = eventTs - offset;
      if (remindAt > now) {
        long delay = remindAt - now;
        ScheduledFuture<?> f = scheduler.schedule(() -> {
          sendReminder(e, offset);
        }, delay, TimeUnit.SECONDS);
        futures.add(f);
      }
    }

    if (eventTs > now) {
      long delay = eventTs - now;
      ScheduledFuture<?> f = scheduler.schedule(() -> {
        sendEventStart(e);
        if (e.periodic && e.periodDays > 0) {
          e.eventTs = e.eventTs + e.periodDays * 24L * 3600L;
          try {
            EventDao.update(e);
          } catch (Exception ex) { ex.printStackTrace(); }
          scheduleRemindersForEvent(e);
        }
      }, delay, TimeUnit.SECONDS);
      futures.add(f);
    }

    scheduledMap.put(e.id, futures);
  }

  private void sendReminder(Event e, long beforeSeconds) {
    if (bot == null) return;
    String when = humanizeOffset(beforeSeconds);
    String text = "Напоминание: событие \"" + e.title + "\" через " + when + "\n" +
        "Когда: " + util.DateTimeUtil.formatEpoch(e.eventTs) + "\n" +
        (e.description != null ? e.description : "");
    SendMessage msg = new SendMessage(String.valueOf(e.chatId), text);
    try {
      bot.execute(msg);
    } catch (TelegramApiException ex) {
      ex.printStackTrace();
    }
  }

  private void sendEventStart(Event e) {
    if (bot == null) return;
    String text = "Событие начинается сейчас: \"" + e.title + "\"\n" +
        "Описание: " + (e.description == null ? "-" : e.description);
    SendMessage msg = new SendMessage(String.valueOf(e.chatId), text);
    try {
      bot.execute(msg);
    } catch (TelegramApiException ex) {
      ex.printStackTrace();
    }
  }

  private String humanizeOffset(long secs) {
    if (secs >= 24L*3600L) {
      long days = secs / (24L*3600L);
      return days + " д.";
    } else if (secs >= 3600L) {
      return (secs/3600L) + " ч.";
    } else if (secs >= 60L) {
      return (secs/60L) + " мин.";
    } else {
      return secs + " сек.";
    }
  }

  public void cancelScheduled(long eventId) {
    List<ScheduledFuture<?>> list = scheduledMap.remove(eventId);
    if (list != null) {
      for (var f : list) f.cancel(false);
    }
  }

  public void scheduleEvent(Event e) {
    scheduleRemindersForEvent(e);
  }
}
