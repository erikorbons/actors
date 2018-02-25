package surge.actors.test;

import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import surge.actors.Actor;
import surge.actors.Filter;
import surge.actors.Path;

import static org.junit.Assert.*;

public class ProbeActor implements Actor {
  private final Path path;
  private final ConcurrentLinkedQueue<TellEvent> events = new ConcurrentLinkedQueue<>();
  private Actor lastSender = null;

  public ProbeActor() {
    this(Path.of(Path.of("$probe"), UUID.randomUUID().toString()));
  }

  public ProbeActor(final Path path) {
    this.path = Objects.requireNonNull(path, "path cannot be null");
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public void tell(final Object message, final Actor sender) {
    events.offer(new TellEvent(message, sender));
  }

  @Override
  public void publish(final Filter filter, final Object message, final Actor sender) {
    events.offer(new PublishEvent(filter, message, sender));
  }

  @Override
  public CompletionStage<Void> awaitTermination(Duration timeout) {
    throw new UnsupportedOperationException("awaitTermination not possible on probe actors");
  }

  @Override
  public <T> void consume(final CompletionStage<T> value) {
    throw new UnsupportedOperationException("consume not possible on probe actor");
  }

  private Optional<TellEvent> getLastEvent() {
    final TellEvent lastEvent = events.poll();

    if (lastEvent != null) {
      assertEvent(lastEvent);

      lastSender = lastEvent.getSender();
    }

    return Optional.ofNullable(lastEvent);
  }

  private void assertEvent(final TellEvent event) {
    assertNotNull("Probe actor " + path + " received a null message", event.getMessage());
    assertNotNull("Probe actor " + path + " was sent a message without a sender",
        event.getSender());

    if (event instanceof PublishEvent) {
      assertNotNull("Probe actor " + path + " received a publish event without a filter",
          ((PublishEvent) event).getFilter());
    }
  }

  public void assertNoMessages(final String message) {
    if (!events.isEmpty()) {
      fail(message);
    }
  }

  public void assertNoMessages() {
    if (!events.isEmpty()) {
      fail("Expected no received messages, got: " + getLastEvent().get());
    }
  }

  public Actor getLastSender() {
    assertNotNull("No messages have been received, there is no last sender", lastSender);
    return lastSender;
  }

  public void reply(final Object message) {
    getLastSender().tell(message, this);
  }

  private Optional<TellEvent> getEvent(final Predicate<TellEvent> predicate) {
    final Iterator<TellEvent> iterator = events.iterator();

    while (iterator.hasNext()) {
      final TellEvent event = iterator.next();

      if (predicate.test(event)) {
        iterator.remove();
        assertEvent(event);
        lastSender = event.getSender();
        return Optional.of(event);
      }
    }

    return Optional.empty();
  }

  public <T> T assertMessage(final Class<T> cls) {
    return getEvent(e -> cls.isAssignableFrom(e.getMessage().getClass()))
        .map(e -> (T) e.getMessage())
        .orElseThrow(() -> new AssertionError(
            "Expected message of type " + cls.getCanonicalName() + " to be delivered to " + path));
  }

  private static class TellEvent {
    private final Object message;
    private final Actor sender;

    public TellEvent(final Object message, final Actor sender) {
      this.message = message;
      this.sender = sender;
    }

    public Object getMessage() {
      return message;
    }

    public Actor getSender() {
      return sender;
    }

    @Override
    public String toString() {
      return "Tell{sender=" + sender + ",message=" + message + "}";
    }
  }

  private static class PublishEvent extends TellEvent {
    private final Filter filter;

    public PublishEvent(final Filter filter, final Object message, final Actor sender) {
      super(message, sender);

      this.filter = filter;
    }

    public Filter getFilter() {
      return filter;
    }

    @Override
    public String toString() {
      return "Publish{filter=" + filter + ",sender=" + getSender() + ",message=" + getMessage()
          + "}";
    }
  }
}
