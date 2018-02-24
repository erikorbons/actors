package surge.actors.bridge.messages;

import java.util.Objects;
import surge.actors.Actor;
import surge.actors.Path;

public final class ExternalMessage {
  private final Path destination;
  private final Object payload;
  private final Actor sender;

  public ExternalMessage(final Path destination, final Object payload, final Actor sender) {
    this.destination = Objects.requireNonNull(destination, "destination cannot be null");
    this.payload = Objects.requireNonNull(payload, "payload cannot be null");
    this.sender = Objects.requireNonNull(sender, "sender cannot be null");
  }

  public Path getDestination() {
    return destination;
  }

  public Object getPayload() {
    return payload;
  }

  public Actor getSender() {
    return sender;
  }
}
