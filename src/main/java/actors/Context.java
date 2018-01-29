package actors;

import java.util.Optional;

public interface Context extends ActorFactory {
  Actor getSelf();
  Optional<Actor> getParent();
  Receiver getReceiver();
  Scheduler getScheduler();
}
