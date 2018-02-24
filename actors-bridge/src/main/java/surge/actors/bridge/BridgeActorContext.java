package surge.actors.bridge;

import java.util.Objects;
import surge.actors.Scheduler;
import surge.actors.actors.LocalActor;
import surge.actors.actors.LocalActorContext;

public class BridgeActorContext extends LocalActorContext {

  private final Node node;

  public BridgeActorContext(final LocalActor parentActor, final Scheduler scheduler,
      final Node node) {
    super(parentActor, scheduler);

    this.node = Objects.requireNonNull(node, "node cannot be null");
  }

  @Override
  protected LocalActorContext createContext(final LocalActor parent, final Scheduler scheduler) {
    return new BridgeActorContext(parent, scheduler, node);
  }
}
