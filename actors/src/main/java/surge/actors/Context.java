package surge.actors;

import java.time.Duration;
import java.util.Optional;

public interface Context extends ActorFactory {
  Actor getSelf();
  Optional<Actor> getParent();
  Receiver getReceiver();
  Scheduler getScheduler();
  Actor spawn(FactoryWithContext entry);
  Receiver stop();
  Receiver kill();
  void setReceiveTimeout(Duration timeout);
  void watch(Actor actorToWatch);
  void unwatch(Actor actorToUnwatch);

  default Actor spawn(Factory entry) {
    return spawn(context -> entry.apply());
  }
}
