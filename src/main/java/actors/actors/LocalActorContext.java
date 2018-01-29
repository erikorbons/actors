package actors.actors;

import actors.Actor;
import actors.Message;
import actors.MessageContext;
import actors.PrivateContext;
import actors.Receiver;
import actors.Scheduler;
import java.util.Objects;
import java.util.Optional;

public class LocalActorContext implements PrivateContext {

  private final Actor parentActor;
  private final Scheduler scheduler;

  // Mutable actor state:
  private Actor self;
  private Receiver currentReceiver;

  public LocalActorContext(final Actor parentActor, final Scheduler scheduler) {
    this.parentActor = parentActor;
    this.scheduler = Objects.requireNonNull(scheduler);

    currentReceiver = Receiver.builder().build();
  }

  @Override
  public void dispatchMessage(final Message message) {
    try {
      System.out.println("Dispatchign: " + message.getPayload());
      getReceiver().receive(message.getPayload(), new LocalMessageContext(message.getSender()));
    } catch (Exception e) {
      // TODO: Handle exceptions while dispatching messages.
      e.printStackTrace();
    }
  }

  /**
   * Perform initialization of mutable actor state after creating the context
   * and the actor itself.
   *
   * @param self The "self" actor that is associated with this context.
   * @param factory The factory method that will be used to create the initial receiver
   *    for this actor.
   */
  public void initialize(final Actor self, final FactoryWithContext factory) {
    this.self = self;
    this.currentReceiver = factory.apply(this);
  }

  @Override
  public Actor getSelf() {
    return self;
  }

  @Override
  public Optional<Actor> getParent() {
    return Optional.ofNullable(parentActor);
  }

  @Override
  public Receiver getReceiver() {
    return currentReceiver;
  }

  @Override
  public Scheduler getScheduler() {
    return scheduler;
  }

  @Override
  public Actor spawn(Factory entry) {
    return null;
  }

  @Override
  public Actor spawn(FactoryWithContext entry) {
    return null;
  }

  private class LocalMessageContext implements MessageContext {
    private final Actor sender;

    public LocalMessageContext(final Actor sender) {
      this.sender = Objects.requireNonNull(sender, "sender cannot be null");
    }

    @Override
    public Actor getSender() {
      return sender;
    }

    @Override
    public Actor getSelf() {
      return LocalActorContext.this.getSelf();
    }

    @Override
    public Optional<Actor> getParent() {
      return LocalActorContext.this.getParent();
    }

    @Override
    public Receiver getReceiver() {
      return LocalActorContext.this.getReceiver();
    }

    @Override
    public Scheduler getScheduler() {
      return LocalActorContext.this.getScheduler();
    }

    @Override
    public Actor spawn(FactoryWithContext entry) {
      return LocalActorContext.this.spawn(entry);
    }
  }
}
