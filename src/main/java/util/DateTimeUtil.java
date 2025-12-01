package util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {
  public static long parseToEpochSeconds(String text) {
    try {
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      LocalDateTime ldt = LocalDateTime.parse(text, dtf);
      return ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
  } catch (Exception ex) {
      try {
        DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate ld = LocalDate.parse(text, dtf2);
        return ld.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
      } catch (Exception e2) {
        throw new IllegalArgumentException("Неверный формат даты. Ожидается yyyy-MM-dd HH-mm или yyyy-MM-dd");
      }
    }
}

public static String formatEpoch(long epochSec) {
  LocalDateTime ldt = LocalDateTime.ofEpochSecond(epochSec, 0, ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
  return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
  }

  public static long nowEpoch() {
    return Instant.now().getEpochSecond();
  }
}
