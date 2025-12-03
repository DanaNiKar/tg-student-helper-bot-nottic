package app;

import bot.StudentHelperBot;
import db.Database;
import service.ReminderService;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
  public static void main(String[] args) throws Exception {
    Database.init();

    ReminderService reminderService = ReminderService.getInstance();

    TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
    String botToken = System.getenv("TG_BOT_TOKEN");
    String botUsername = System.getenv("TG_BOT_USERNAME");

    if (botToken == null || botUsername == null) {
      System.err.println("Please set TG_BOT_TOKEN AND TG_BOT_USERNAME environment variables");
      System.exit(1);
    }

    StudentHelperBot bot = new StudentHelperBot(botUsername, botToken, reminderService);
    botsApi.registerBot(bot);

    reminderService.reloadAllReminders();

    System.out.println("Bot started");
  }
}