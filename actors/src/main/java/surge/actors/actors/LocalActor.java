package surge.actors.actors;

import surge.actors.Actor;
import surge.actors.ActorFactory;
import surge.actors.Filter;
import surge.actors.Mailbox;
import surge.actors.Message;
import surge.actors.Path;
import surge.actors.Scheduler;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class LocalActor implements Actor {
  private final Path path;
  private final Mailbox mailbox;

  public LocalActor(final Path path, final Mailbox mailbox) {
    this.path = Objects.requireNonNull(path, "path cannot be null");
    this.mailbox = Objects.requireNonNull(mailbox, "mailbox cannot be null");
  }

  public static ActorFactory factory(final Scheduler scheduler) {
    return new LocalActorFactory(scheduler);
  }

  @Override
  public Path getPath() {
    return path;
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

      @Override
      public Optional<Filter> getPublishFilter() {
        return Optional.empty();
      }

      @Override
      public PublishMode getPublishMode() {
        return PublishMode.LOCAL_AND_PUBLISH;
      }
    });
  }

  @Override
  public void publish(Filter filter, Object message, Actor sender) {
    Objects.requireNonNull(filter, "filter cannot be null");
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

      @Override
      public PublishMode getPublishMode() {
        return PublishMode.PUBLISH_ONLY;
      }

      @Override
      public Optional<Filter> getPublishFilter() {
        return Optional.of(filter);
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

      @Override
      public Optional<Filter> getPublishFilter() {
        return Optional.empty();
      }

      @Override
      public PublishMode getPublishMode() {
        return PublishMode.LOCAL_AND_PUBLISH;
      }
    });
  }

  @Override
  public String toString() {
    return "LocalActor[" + getPath().getFullName() + "]";
  }
}
