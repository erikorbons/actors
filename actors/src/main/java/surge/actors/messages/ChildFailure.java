package surge.actors.messages;

import java.util.Objects;

/**
 * Notifies a supervisor that an actor has failed.
 */
public class ChildFailure {
  private final Exception cause;

  public ChildFailure(final Exception cause) {
    this.cause = Objects.requireNonNull(cause, "cause cannot be null");
  }

  public Exception getCause() {
    return cause;
  }
}
