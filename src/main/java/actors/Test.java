package actors;

import actors.actors.LocalActor;
import actors.dispatchers.ExecutorServiceScheduler;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Test {

  public static Receiver testFactory() {
    return Receiver.builder()
        .match(String.class, (s, ctx) -> { })
        .build();
  }

  public static void main(final String[] args) {
    final Scheduler scheduler = new ExecutorServiceScheduler(new ThreadPoolExecutor(1, 20, 30,
        TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000, true)));

    scheduler.schedule(() -> {
      System.out.println("Scheduler executes");
    });

    final Actor actor = LocalActor.factory(scheduler).spawn((context) -> {
      return Receiver.builder()
          .match(String.class, (msg, ctx) -> {
            System.out.println("Received: " + msg);
          })
          .build();
    });

    actor.tell("Hello, World!", actor);
  }

}
