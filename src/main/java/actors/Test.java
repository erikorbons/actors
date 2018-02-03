package actors;

import actors.dispatchers.ExecutorServiceScheduler;
import actors.messages.Stop;
import java.util.concurrent.ForkJoinPool;

public class Test {

  public static Receiver testFactory() {
    return Receiver.builder()
        .match(String.class, (s, ctx) -> { })
        .build();
  }

  public static void main(final String[] args) {
    final ForkJoinPool executor = new ForkJoinPool(
        Runtime.getRuntime().availableProcessors(),
        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
        null,
        true
    );
    final Scheduler scheduler = new ExecutorServiceScheduler(executor);

    scheduler.schedule(() -> {
      System.out.println("Scheduler executes");
    });

    final Actor actor = actors.actors.LocalActor.factory(scheduler).spawn((context) -> {
      return Receiver.builder()
          .match(String.class, (msg, ctx) -> {
            System.out.println("Main actor received: " + msg);

            for (int i = 0; i < 100; ++ i) {
              final String actorName = "child-" + i;
              final Actor childActor = ctx.spawn(() -> childActor(actorName, true), actorName);
              childActor.tell("Hello, World! (for child)", ctx.getSender());
            }
          })
          .build();
    }, "root");

    actor.tell("Hello, World!", actor);
    actor.tell(new Stop(), actor);

    try {
      Thread.sleep(Long.MAX_VALUE);
    } catch (Exception e) {
    }
  }

  public static Receiver childActor(final String name, final boolean startChild) {
    return Receiver.builder()
        .match(String.class, (msg, ctx) -> {
          if (startChild) {
            final String actorName = name + "-0";
            final Actor childActor = ctx.spawn(() -> childActor(actorName, false), actorName);
            childActor.tell("Hello, World! (for child of child)", ctx.getSelf());
          }
          Thread.sleep((long)(Math.random() * 500));
          System.out.println(Thread.currentThread().getId() + ": Child actor " + name + " received: " + msg);

        })
        .build();
  }

}
