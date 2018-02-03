package actors.actors;

import actors.Actor;
import actors.Message;
import actors.MessageContext;
import actors.PrivateContext;
import actors.Receiver;
import actors.Scheduler;
import actors.messages.ChildFailure;
import actors.messages.MailboxSuspend;
import actors.messages.MailboxTerminate;
import actors.messages.Restart;
import actors.messages.Stop;
import actors.messages.ChildTerminated;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class LocalActorContext extends LocalActorFactory implements PrivateContext {

  private final LocalActor parentActor;

  private final Map<String, LocalActor> children = new HashMap<>();

  // Mutable actor state:
  private LocalActor self;
  private Receiver currentReceiver;

  public LocalActorContext(final LocalActor parentActor, final Scheduler scheduler) {
    super(scheduler);
    this.parentActor = parentActor;

    currentReceiver = Receiver.builder().build();
  }

  @Override
  public boolean dispatchMessage(final Message message) {
    try {
      currentReceiver = getReceiver()
          .receive(message.getPayload(), new LocalMessageContext(message.getSender()))
          .orElseGet(() -> handleSystemMessages(message));
      return true;
    } catch (Exception e) {
      handleFailure(e);
      return false;
    }
  }

  private Receiver handleSystemMessages(final Message message) {
    final Object payload = message.getPayload();

    if (payload instanceof ChildFailure) {
      // A failure was received for a child. Notify the child that it should
      // restart:
      ((LocalActor) message.getSender()).tellSystem(new Restart(), getSelf());
    } else if (payload instanceof Restart) {

    } else if (payload instanceof Stop) {
      return stopping();
    } else if (payload instanceof ChildTerminated) {

    }

    return currentReceiver;
  }

  private Receiver stopping() {
    System.out.println("Stopping: " + getSelf().getName());

    // Notify the mailbox that it should be suspended:
    self.tellSystem(new MailboxSuspend(), getSelf());

    // Notify all children that they should stop. Use normal message flow,
    // let the children process previous messages:
    if (children.isEmpty()) {
      return stopped();
    } else {
      children.values().stream()
          .forEach(childActor -> childActor.tell(new Stop(), getSelf()));
    }

    // Await termination of all the children, then terminate self:
    return Receiver.builder()
        .match(ChildTerminated.class, (msg, context) -> {
          // Remove the child:
          children.remove(context.getSender().getName());

          if (children.isEmpty()) {
            return stopped();
          }

          return children.isEmpty()
              ? stopped()
              : context.getReceiver();
        })
        .build();
  }

  private Receiver stopped() {
    System.out.println("Stopped: " + getSelf().getName());

    // Terminate the mailbox:
    self.tellSystem(new MailboxTerminate(), getSelf());

    // Notify the parent that this actor has terminated:
    if (parentActor != null) {
      parentActor.tellSystem(new ChildTerminated(), getSelf());
    }

    return Receiver.builder().build();
  }

  private void handleFailure(final Exception cause) {
    // Notify the mailbox that message delivery should be suspended:
    self.tellSystem(new MailboxSuspend(), getSelf());

    // Notify the supervisor of the failure:
    if (parentActor != null) {
      parentActor.tellSystem(new ChildFailure(cause), getSelf());
    }

    cause.printStackTrace();
  }

  /**
   * Perform initialization of mutable actor state after creating the context
   * and the actor itself.
   *
   * @param self The "self" actor that is associated with this context.
   * @param factory The factory method that will be used to create the initial receiver
   *    for this actor.
   */
  public void initialize(final LocalActor self, final FactoryWithContext factory) {
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
  public LocalActor spawn(final FactoryWithContext entry) {
    return spawn(entry, "child-" + UUID.randomUUID().toString());
  }

  @Override
  public LocalActor spawn(final FactoryWithContext entry, final String name) {
    Objects.requireNonNull(name, "name cannot be null");

    final LocalActor actor = super.spawn(entry, name);

    children.put(name, actor);

    return actor;
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
    public Actor spawn(final FactoryWithContext entry) {
      return LocalActorContext.this.spawn(entry);
    }

    @Override
    public Actor spawn(final FactoryWithContext entry, final String name) {
      return LocalActorContext.this.spawn(entry, name);
    }
  }
}
