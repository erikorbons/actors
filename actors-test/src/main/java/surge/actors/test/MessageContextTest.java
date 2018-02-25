package surge.actors.test;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import surge.actors.Actor;
import surge.actors.MessageContext;
import surge.actors.Receiver;
import surge.actors.Scheduler;

public class MessageContextTest implements MessageContext {

  private final TestContext context;
  private final Actor sender;

  public MessageContextTest(final TestContext context, final Actor sender) {
    this.context = Objects.requireNonNull(context, "context cannot be null");
    this.sender = Objects.requireNonNull(sender, "sender cannot be null");
  }

  @Override
  public Actor getSender() {
    return sender;
  }

  @Override
  public Actor getSelf() {
    return context.getSelf();
  }

  @Override
  public Optional<Actor> getParent() {
    return context.getParent();
  }

  @Override
  public Receiver getReceiver() {
    return context.getReceiver();
  }

  @Override
  public Scheduler getScheduler() {
    return context.getScheduler();
  }

  @Override
  public Actor spawn(FactoryWithContext entry) {
    return context.spawn(entry);
  }

  @Override
  public Receiver stop() {
    return context.stop();
  }

  @Override
  public Receiver kill() {
    return context.kill();
  }

  @Override
  public void setReceiveTimeout(Duration timeout) {
    context.setReceiveTimeout(timeout);
  }

  @Override
  public void watch(Actor actorToWatch) {
    context.watch(actorToWatch);
  }

  @Override
  public void unwatch(Actor actorToUnwatch) {
    context.unwatch(actorToUnwatch);
  }

  @Override
  public Actor spawn(FactoryWithContext entry, String name) {
    return context.spawn(entry, name);
  }
}
