package surge.actors.test;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import surge.actors.Actor;
import surge.actors.Context;
import surge.actors.Receiver;
import surge.actors.Scheduler;

public class TestContext implements Context {
  private final ReceiverTest receiver;
  private final TestScheduler scheduler;
  private final Actor self;
  private final Actor parent;

  public TestContext(final ReceiverTest receiver, final TestScheduler scheduler, final Actor self, final Actor parent) {
    this.receiver = Objects.requireNonNull(receiver, "receiver cannot be null");
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler cannot be null");
    this.self = Objects.requireNonNull(self, "self cannot be null");
    this.parent = parent;
  }

  @Override
  public Actor getSelf() {
    return self;
  }

  @Override
  public Optional<Actor> getParent() {
    return Optional.ofNullable(parent);
  }

  @Override
  public Receiver getReceiver() {
    return receiver.getCurrentReceiver();
  }

  @Override
  public Scheduler getScheduler() {
    return scheduler;
  }

  @Override
  public Actor spawn(FactoryWithContext entry) {
    return null;
  }

  @Override
  public Receiver stop() {
    return null;
  }

  @Override
  public Receiver kill() {
    return null;
  }

  @Override
  public void setReceiveTimeout(Duration timeout) {

  }

  @Override
  public void watch(Actor actorToWatch) {

  }

  @Override
  public void unwatch(Actor actorToUnwatch) {

  }

  @Override
  public Actor spawn(FactoryWithContext entry, String name) {
    return null;
  }
}
