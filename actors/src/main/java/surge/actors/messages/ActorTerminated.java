package surge.actors.messages;

import surge.actors.Actor;

public final class ActorTerminated {
  private final Actor actor;

  public ActorTerminated(final Actor actor) {
    this.actor = actor;
  }

  public Actor getActor() {
    return actor;
  }
}
