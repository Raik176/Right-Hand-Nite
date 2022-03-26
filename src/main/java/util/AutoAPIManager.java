package util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class AutoAPIManager {
    public AutoAPIManager(Runnable function) {
        new Thread(() -> {
            while (true) {
                try {
                    function.run();
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime nextHour = now.plusHours(1).truncatedTo(ChronoUnit.HOURS);
                    Thread.sleep(Duration.between(now,nextHour).toMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
