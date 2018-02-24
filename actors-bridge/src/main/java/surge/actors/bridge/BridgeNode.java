package surge.actors.bridge;

import java.util.Objects;
import surge.actors.Actor;
import surge.actors.ActorFactory;
import surge.actors.ActorFactory.FactoryWithContext;
import surge.actors.bridge.actors.MessageDispatcher;

public class BridgeNode {

  private final Actor mainActor;
  private final Actor messageDispatcher;

  public BridgeNode(final ActorFactory actorFactory, final FactoryWithContext mainState) {
    mainActor = actorFactory
        .spawn(Objects.requireNonNull(mainState, "mainState cannot be null"), "main");
    messageDispatcher = actorFactory
        .spawn(() -> MessageDispatcher.forActor(mainActor), "dispatcher");
  }


}
