package actors.actors;

import actors.Actor;
import actors.ActorFactory;
import actors.Mailbox;
import actors.Message;
import actors.Scheduler;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

public class LocalActor implements Actor {
  private final String name;
  private final Mailbox mailbox;

  public LocalActor(final String name, final Mailbox mailbox) {
    this.name = Objects.requireNonNull(name, "name cannot be null");
    this.mailbox = Objects.requireNonNull(mailbox, "mailbox cannot be null");
  }

  public static ActorFactory factory(final Scheduler scheduler) {
    return new LocalActorFactory(scheduler);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void tell(final Object message, final Actor sender) {
    Objects.requireNonNull(message, "message cannot be null");
    Objects.requireNonNull(sender, "sender cannot be null");

    mailbox.enqueue(new Message() {
      @Override
      public Object getPayload() {
        return message;
      }

      @Override
      public Actor getSender() {
        return sender;
      }
    });
  }

  @Override
  public CompletionStage<Void> awaitTermination(final Duration timeout) {
    // TODO: implement.
    return null;
  }

  @Override
  public <T> void consume(final CompletionStage<T> value) {
    Objects.requireNonNull(value, "value cannot be null");

    value.handle((result, error) -> {
      if (error != null) {
        tell(error, this);
      } else {
        tell(result, this);
      }
      return null;
    });
  }

  public void tellSystem(final Object message, final Actor sender) {
    Objects.requireNonNull(message, "message cannot be null");
    Objects.requireNonNull(sender, "sender cannot be null");

    mailbox.enqueueSystemMessage(new Message() {
      @Override
      public Object getPayload() {
        return message;
      }

      @Override
      public Actor getSender() {
        return sender;
      }
    });
  }
}
