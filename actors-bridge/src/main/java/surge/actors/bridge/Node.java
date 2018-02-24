package surge.actors.bridge;

import java.util.concurrent.Flow;
import surge.actors.Message;
import surge.actors.Path;
import surge.actors.Scheduler;
import surge.actors.actors.LocalActor;
import surge.actors.actors.LocalActorContext;
import surge.actors.actors.LocalActorFactory;

public class Node extends LocalActorFactory {
  public Node(final Scheduler scheduler) {
    super(scheduler);
  }

  public void deliver(final Path path, final Message message) {

  }

  @Override
  protected LocalActorContext createContext(final LocalActor parent, final Scheduler scheduler) {
    return new BridgeActorContext(parent, scheduler, this);
  }

  /**
   * Returns a subscriber that when subscribed to a publisher receives
   * messages that need to be dispatched to an actor under control of this node.
   *
   * @return
   */
  public Flow.Subscriber<Object> getMessageReceiver() {
    return null;
  }

  /**
   * Returns a producer that when subscribed to produces messages that need to be delivered to
   * external nodes.
   *
   * @return
   */
  public Flow.Publisher<Object> getMessageProducer() {
    return null;
  }
}
