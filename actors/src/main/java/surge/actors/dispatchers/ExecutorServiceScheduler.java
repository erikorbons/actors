package surge.actors.dispatchers;

import surge.actors.Scheduler;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ExecutorServiceScheduler implements Scheduler {

  private final ExecutorService executorService;
  private final ScheduledExecutorService scheduledExecutorService;

  public ExecutorServiceScheduler(final ExecutorService executorService,
      final ScheduledExecutorService scheduledExecutorService) {
    this.executorService = Objects.requireNonNull(executorService, "executorService cannot be null");
    this.scheduledExecutorService = Objects.requireNonNull(scheduledExecutorService, "scheduledExecutorService cannot be null");
  }

  @Override
  public void schedule(final Runnable runnable) {
    executorService.execute(runnable);
  }

  @Override
  public ScheduledFuture<?> schedule(final Runnable runnable, final Duration delay) {
    return scheduledExecutorService
        .schedule(runnable, delay.get(ChronoUnit.NANOS), TimeUnit.NANOSECONDS);
  }
}
