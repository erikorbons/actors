package actors;

import java.util.Optional;

public interface Context extends ActorFactory {
  Actor getSelf();
  Optional<Actor> getParent();
  Receiver getReceiver();
  Scheduler getScheduler();
  Actor spawn(FactoryWithContext entry);

  default Actor spawn(Factory entry) {
    return spawn(context -> entry.apply());
  }
}
