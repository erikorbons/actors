package surge.actors.test;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import surge.actors.Scheduler;

public class TestScheduler implements Scheduler {

  @Override
  public void schedule(Runnable runnable) {

  }

  @Override
  public ScheduledFuture<?> schedule(Runnable runnable, Duration delay) {
    return null;
  }
}
