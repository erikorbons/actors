package actors;

import actors.actors.Ask;
import actors.messages.SpawnChild;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface Actor {
  Path getPath();
  void tell(Object message, Actor sender);
  void publish(Filter filter, Object message, Actor sender);

  default CompletionStage<Object> ask(final Actor target, final Object message,
      final Duration timeout) {
    Objects.requireNonNull(target, "target cannot be null");
    Objects.requireNonNull(message, "message cannot be null");
    Objects.requireNonNull(timeout, "timeout cannot be null");

    final CompletableFuture<Object> future = new CompletableFuture<>();
    final String name = "$ask-" + UUID.randomUUID().toString();

    // Spawn a child of the target actor to handle the ask:
    tell(
        new SpawnChild(
            context -> Ask.ask(target, message, timeout, future, context),
            name
        ),
        this
    );

    return future;
  }

  default CompletionStage<Object> ask(final Object message, final Duration timeout) {
    return ask(this, message, timeout);
  }

  CompletionStage<Void> awaitTermination(Duration timeout);
  <T> void consume(CompletionStage<T> value);
}
