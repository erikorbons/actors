package actors.actors;

import actors.Actor;
import actors.Context;
import actors.Receiver;
import actors.messages.ReceiveTimeout;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public abstract class Ask {

  public static Receiver ask(final Actor target, final Object message, final Duration timeout,
      final CompletableFuture<Object> future, final Context context) {

    // Send the message to the target:
    target.tell(message, context.getSelf());

    // Specify a receive timeout:
    context.setReceiveTimeout(Objects.requireNonNull(timeout, "timeout cannot be null"));

    return Receiver.builder()
        .match(ReceiveTimeout.class, (t, ctx) -> {
          // Complete the future with a timeout exception:
          ctx.getScheduler().schedule(() -> {
            future.completeExceptionally(new TimeoutException());
          });

          // Stop this actor:
          System.out.println("Killing ask after: " + t);
          return ctx.stop();
        })
        .matchAny((result, ctx) -> {
          System.out.println("Ask received: " + result);
          // Complete the future in a separate job:
          ctx.getScheduler().schedule(() -> {
            future.complete(result);
          });

          // Stop this actor:
          System.out.println("Killing ask after: " + result);
          return ctx.stop();
        })
        .build();

  }

}
