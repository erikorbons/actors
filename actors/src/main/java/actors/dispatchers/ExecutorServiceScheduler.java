package actors.dispatchers;

import actors.Scheduler;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class ExecutorServiceScheduler implements Scheduler {

  private final ExecutorService executorService;

  public ExecutorServiceScheduler(final ExecutorService executorService) {
    this.executorService = Objects.requireNonNull(executorService, "executorService cannot be null");
  }

  @Override
  public void schedule(final Runnable runnable) {
    executorService.execute(runnable);

  }
}
