package actors;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

public interface Scheduler {
  void schedule(Runnable runnable);
  ScheduledFuture<?> schedule(Runnable runnable, Duration delay);
}
