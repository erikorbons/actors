package surge.actors.messages;

import java.util.Objects;
import surge.actors.Actor;

public final class Watching {
  private final Actor actor;

  public Watching(final Actor actor) {
    this.actor = Objects.requireNonNull(actor, "actor cannot be null");
  }

  public Actor getActor() {
    return actor;
  }
}
