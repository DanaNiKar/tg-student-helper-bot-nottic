package bot;

import dao.EventDao;
import dao.TaskDao;
import dao.ScheduleDao;
import model.Event;
import model.Task;
import model.ScheduleEntry;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import service.ReminderService;
import util.DateTimeUtil;

import java.sql.SQLException;
import java.util.List;

public class StudentHelperBot extends TelegramLongPollingBot {
  private final String username;
  private final String token;
  private final ReminderService reminderService;

  public StudentHelperBot(String username, String token, ReminderService service) {
    this.username = username;
    this.token = token;
    this.reminderService = service;
    this.reminderService.setBot(this);
  }

  @Override
  public String getBotUsername() {
    return username;
  }

  @Override
  public String getBotToken() {
    return token;
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (!update.hasMessage() || !update.getMessage().hasText()) return;
    String txt = update.getMessage().getText().trim();
    long chatId = update.getMessage().getChatId();

    try {
      if (txt.startsWith("/start")) {
        send(chatId, "Привет! Я бот-помощник студента. Доступные команды:\n" +
            "/add_task title | description | yyyy-MM-dd HH:mm\n" +
            "/list_tasks\n" +
            "/done_task id\n" +
            "/add_event title | description | yyyy-MM-dd HH:mm\n" +
            "/list_events\n" +
            "/move_event id | yyyy-MM-dd HH:mm\n" +
            "/make_periodic id | days (e.g. 7)\n" +
            "/add_schedule dayOfweek(1-7) | HH:mm | title | place\n" +
            "/list_schedule\n" +
            "/edit_schedule id | day | HH:mm | title | place\n" +
            "\nПримеры:\n" +
            "/add_task Домашняя работа по математике | решить задачи 1-10 | 2025-12-15 23:59");
      }
      else if (txt.startsWith("/add_task")) {
        // parse
        String body = txt.substring("/add_task".length()).trim();
        String[] parts = body.split("\\|");
        if (parts.length < 3) {
          send(chatId, "Неверный формат. /add_task title | description | yyyy-MM-dd HH:mm");
        } else {
          Task t = new Task();
          t.chatId = chatId;
          t.title = parts[0].trim();
          t.description = parts[1].trim();
          t.deadlineTs = DateTimeUtil.parseToEpochSeconds(parts[2].trim());
          t.done = false;
          TaskDao.add(t);
          send(chatId, "Задача добавлена с id=" + t.id);
        }
      }
      else if (txt.startsWith("/list_tasks")) {
        List<Task> tasks = TaskDao.list(chatId);
        if (tasks.isEmpty()) send(chatId, "Список задач пуст");
        else {
          StringBuilder sb = new StringBuilder();
          for (Task t : tasks) {
            long now = DateTimeUtil.nowEpoch();
            long daysLeft = (t.deadlineTs - now) / ( 24 * 3600);
            String urgent = (!t.done && (t.deadlineTs - now) <= 7 * 24 * 3600) ? " [СРОЧНО]" : "";
            sb.append("id:").append(t.id).append(urgent).append("\n")
                .append(t.title).append("\n")
                .append("Дедлайн: ").append(DateTimeUtil.formatEpoch(t.deadlineTs)).append("\n")
                .append("Выполнено: ").append(t.done ? "да" : "нет").append("\n\n");
          }
          send(chatId, sb.toString());
        }
      }
      else if (txt.startsWith("/done_task")) {
        String body = txt.substring("/done_task".length()).trim();
        long id = Long.parseLong(body);
        TaskDao.markDone(id, true);
        send(chatId, "Отмечено как выполненное: id=" + id);
      }
      else if (txt.startsWith("/add_event")) {
        String body = txt.substring("/add_event".length()).trim();
        String[] parts = body.split("\\|");
        if (parts.length < 3) {
          send(chatId, "Неверный формат. /add_event title | description | yyyy-MM-dd HH:mm");
        } else {
          Event e = new Event();
          e.chatId = chatId;
          e.title = parts[0].trim();
          e.description = parts[1].trim();
          e.eventTs = DateTimeUtil.parseToEpochSeconds(parts[2].trim());
          e.periodic = false;
          e.periodDays = 0;
          EventDao.add(e);
          reminderService.scheduleEvent(e);
          send(chatId, "Событие добавлено id=" + e.id);
        }
      }
      else if (txt.startsWith("/list_events")) {
        List<Event> events = EventDao.listUpcoming(chatId);
        if (events.isEmpty()) send(chatId, "Нет предстоящих событий");
        else {
          StringBuilder sb = new StringBuilder();
          for (Event e : events) {
            sb.append("id:").append(e.id).append(e.periodic ? " [повторяется]" : "").append("\n")
                .append(e.title).append("\n")
                .append(DateTimeUtil.formatEpoch(e.eventTs)).append("\n")
                .append(e.description == null ? "" : e.description).append("\n\n");
          }
          send(chatId, sb.toString());
        }
      }
      else if (txt.startsWith("/move_event")) {
        String body = txt.substring("/move_event".length()).trim();
        String[] parts = body.split("\\|");
        if (parts.length < 2) send(chatId, "Неверный формат. /move_event id | yyyy-MM-dd HH:mm");
        else {
          long id = Long.parseLong(parts[0].trim());
          long ts = DateTimeUtil.parseToEpochSeconds(parts[1].trim());
          Event e = EventDao.findById(id);
          if (e == null) send(chatId, "Событие не найдено");
          else {
            e.eventTs = ts;
            EventDao.update(e);
            reminderService.scheduleEvent(e);
            send(chatId, "Событие перемещено");
          }
        }
      }
      else if (txt.startsWith("/make_periodic")) {
        String body = txt.substring("/make_periodic".length()).trim();
        String[] parts = body.split("\\|");
        if (parts.length < 2) send(chatId, "Неверный формат. /make periodic id | days");
        else {
          long id = Long.parseLong(parts[0].trim());
          int days = Integer.parseInt(parts[1].trim());
          Event e = EventDao.findById(id);
          if (e == null) send(chatId, "Событие не найдено");
          else {
            e.periodic = true;
            e.periodDays = days;
            EventDao.update(e);
            reminderService.scheduleEvent(e);
            send(chatId, "Сделано периодическим каждые " + days + " дней");
          }
        }
      }
      else if (txt.startsWith("/add_schedule")) {
        String body = txt.substring("/add_schedule".length()).trim();
        String[] p = body.split("\\|");
        if (p.length < 4) send(chatId, "Неверный формат. /add_schedule day(1-7) | HH:mm | title | place");
        else {
          ScheduleEntry e = new ScheduleEntry();
          e.chatId = chatId;
          e.dayOfWeek = Integer.parseInt(p[0].trim());
          e.time = p[1].trim();
          e.title = p[2].trim();
          e.location = p[3].trim();
          e.active = true;
          ScheduleDao.add(e);
          send(chatId, "Запись расписания добавлена id=" + e.id);
        }
      }
      else if (txt.startsWith("/list_schedule")) {
        List<ScheduleEntry> list = ScheduleDao.listForChat(chatId);
        if (list.isEmpty()) send(chatId, "Расписание пустое");
        else {
          StringBuilder sb = new StringBuilder();
          for (ScheduleEntry s : list) {
          sb.append("id:").append(s.id).append("\n")
              .append("День: ").append(s.dayOfWeek).append(" Время: ").append(s.time).append("\n")
              .append(s.title).append(" @ ").append(s.location).append("\n\n");
          }
          send(chatId, sb.toString());
        }
      }
      else if (txt.startsWith("/edit_schedule")) {
        String body = txt.substring("/edit_schedule".length()).trim();
        String[] p = body.split("\\|");
        if (p.length < 5) send(chatId, "Неверный формат. /edit_schedule id | day | HH:mm | title | place");
        else {
          long id = Long.parseLong(p[0].trim());
          ScheduleEntry e = new ScheduleEntry();
          e.id = id;
          e.chatId = chatId;
          e.dayOfWeek = Integer.parseInt(p[1].trim());
          e.time = p[2].trim();
          e.title = p[3].trim();
          e.location = p[4].trim();
          e.active = true;
          ScheduleDao.update(e);
          send(chatId, "Запись изменена");
        }
      }
      else {
        send(chatId, "Неизвестная команда. Напишите /start для списка команды");
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
      send(chatId, "Ошибка базы данных: " + ex.getMessage());
    } catch (Exception ex) {
      ex.printStackTrace();
      send(chatId, "Ошибка: " + ex.getMessage());
    }
  }

  private void send(long chatId, String text) {
    SendMessage m = new SendMessage(String.valueOf(chatId), text);
    try { execute(m); } catch (Exception ex) { ex.printStackTrace(); }
  }
}
