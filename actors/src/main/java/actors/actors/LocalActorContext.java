package actors.actors;

import actors.Actor;
import actors.Message;
import actors.MessageContext;
import actors.PrivateContext;
import actors.Receiver;
import actors.Receiver.FailureAction;
import actors.Scheduler;
import actors.messages.ChildFailure;
import actors.messages.ChildRestarted;
import actors.messages.Kill;
import actors.messages.MailboxSuspend;
import actors.messages.MailboxTerminate;
import actors.messages.MailboxUnsuspend;
import actors.messages.ReceiveTimeout;
import actors.messages.Restart;
import actors.messages.SpawnChild;
import actors.messages.Stop;
import actors.messages.ChildTerminated;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LocalActorContext extends LocalActorFactory implements PrivateContext {

  private final LocalActor parentActor;

  private final Map<String, LocalActor> children = new HashMap<>();

  // Mutable actor state:
  private FactoryWithContext initialFactory;
  private LocalActor self;
  private Receiver currentReceiver;
  private Duration receiveTimeoutDuration = null;
  private ScheduledFuture<?> receiveTimeout = null;

  public LocalActorContext(final LocalActor parentActor, final Scheduler scheduler) {
    super(scheduler);
    this.parentActor = parentActor;

    currentReceiver = Receiver.builder().build();
  }

  @Override
  public boolean dispatchMessage(final Message message) {
    try {
      // Handle system messages first:
      final Optional<Receiver> systemReceiver = handleSystemMessages(message);
      if (systemReceiver.isPresent()) {
        currentReceiver = systemReceiver.get();
        return true;
      }

      // Handle "normal" messages and system messages whose handling can be overridden:
      currentReceiver = getReceiver()
            .receive(message.getPayload(), new LocalMessageContext(message.getSender()))
            .orElseGet(() -> handleOverridableSystemMessages(message));

      // Reset the receive timeout, if one is available:
      if (receiveTimeoutDuration != null) {
        setReceiveTimeout(receiveTimeoutDuration);
      }

      return true;
    } catch (Exception e) {
      handleFailure(e);
      return false;
    }
  }

  private Optional<Receiver> handleSystemMessages(final Message message) throws Exception {
    final Object payload = message.getPayload();

    if (payload instanceof ChildTerminated) {
      // Remove the child from the child mapping:
      children.remove(message.getSender().getName());

      // Don't return a new receiver, let the non-system receiver handle the
      // message as well:
      return Optional.empty();
    } else if (payload instanceof Kill) {
      return Optional.of(kill());
    } else if (payload instanceof ChildFailure) {
      handleChildFailure((LocalActor) message.getSender(), ((ChildFailure) payload).getCause());

      // Don't return a new receiver, allow the non-system receiver to see the message
      // as well:
      return Optional.empty();
    } else if (payload instanceof Restart) {
      // Suspend the mailbox for this actor:
      self.tellSystem(new MailboxSuspend(), self);

      // Invoke the restart handler:
      getReceiver().beforeRestart(this);

      // Stop all children, then restart:
      return Optional.of(stopChildren(child -> child.tellSystem(new Kill(), getSelf()), this::restart));
    } else if (payload instanceof ReceiveTimeout) {
      if (receiveTimeoutDuration != null) {
        // If a receive timeout is still active, reset it and let the current receiver
        // handle the timeout:
        this.receiveTimeoutDuration = null;
        this.receiveTimeout = null;
        return Optional.empty();
      } else {
        // If a receive timeout is not currently active, don't report the message.
        // This may happen if the receive timeout got cancelled while the timeout
        // message was still scheduled for execution.
        return Optional.of(getReceiver());
      }
    }

    return Optional.empty();
  }

  private Receiver handleOverridableSystemMessages(final Message message) {
    final Object payload = message.getPayload();

    if (payload instanceof Restart) {

    } else if (payload instanceof Stop) {
      return stop();
    } else if (payload instanceof SpawnChild) {
      spawn(((SpawnChild) payload).getFactory(), ((SpawnChild) payload).getName());
    }

    return currentReceiver;
  }

  private void handleChildFailure(final LocalActor child, final Exception cause) throws Exception {
    System.out.println("Child failed: " + child.getName());

    final FailureAction action = getReceiver().handleFailure(cause)
        .orElse(FailureAction.RESTART);

    switch (action) {
      case RESTART:
        child.tellSystem(new Restart(), getSelf());
        break;
      case RESUME:
        child.tellSystem(new MailboxUnsuspend(), getSelf());
        break;
      case KILL:
        child.tellSystem(new Kill(), getSelf());
        break;
      default:
      case ESCALATE:
        // Re-throw the cause
        throw cause;
    }
  }

  private Receiver stopChildren(final Consumer<LocalActor> stopHandler, final Supplier<Receiver> continuation) {
    System.out.println("Stopping: " + getSelf().getName());

    // Terminate any timers:
    if (receiveTimeout != null) {
      receiveTimeout.cancel(false);
      receiveTimeoutDuration = null;
    }

    // Notify the mailbox that it should be suspended:
    self.tellSystem(new MailboxSuspend(), getSelf());

    // Notify all children that they should stop. Use normal message flow,
    // let the children process previous messages:
    if (children.isEmpty()) {
      return continuation.get();
    } else {
      children.values().stream()
          .forEach(stopHandler);
    }

    // Await termination of all the children, then terminate self:
    return Receiver.builder()
        .match(ChildTerminated.class, (msg, context) -> {
          if (children.isEmpty()) {
            return continuation.get();
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

  private Receiver restart() {
    currentReceiver = initialFactory.apply(this);

    getReceiver().afterRestart(this);

    // Unsuspend the mailbox:
    self.tellSystem(new MailboxUnsuspend(), self);

    // Notify the parent that the actor has restarted:
    if (parentActor != null) {
      parentActor.tellSystem(new ChildRestarted(), getSelf());
    }

    return getReceiver();
  }

  private void handleFailure(final Exception cause) {
    // Notify the mailbox that message delivery should be suspended:
    self.tellSystem(new MailboxSuspend(), getSelf());

    // Notify the supervisor of the failure:
    System.out.println("Notify parent of failure (1)");
    if (parentActor != null) {
      System.out.println("Notify parent of failure (2)");
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
    this.initialFactory = factory;
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

    final LocalActor actor = super.spawn(self, entry, name);

    children.put(name, actor);

    return actor;
  }

  @Override
  public Receiver stop() {
    return stopChildren(actor -> actor.tell(new Stop(), getSelf()), this::stopped);
  }

  @Override
  public Receiver kill() {
    return stopChildren(actor -> actor.tellSystem(new Kill(), getSelf()), this::stopped);
  }

  @Override
  public void setReceiveTimeout(final Duration timeout) {
    if (receiveTimeout != null) {
      receiveTimeout.cancel(false);
    }

    // Reset the timeout if "zero" is provided:
    if (timeout.isZero()) {
      this.receiveTimeoutDuration = null;
      return;
    }

    this.receiveTimeoutDuration = timeout;

    final Actor self = getSelf();

    System.out.println("Scheduling timeout for " + getSelf().getName() + ": " + timeout);

    receiveTimeout = getScheduler().schedule(
        () -> {
          self.tell(new ReceiveTimeout(), self);
        },
        Objects.requireNonNull(timeout, "timeout cannot be null")
    );
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

    @Override
    public Receiver stop() {
      return LocalActorContext.this.stop();
    }

    @Override
    public Receiver kill() {
      return LocalActorContext.this.kill();
    }

    @Override
    public void setReceiveTimeout(final Duration timeout) {
      LocalActorContext.this.setReceiveTimeout(timeout);
    }
  }
}
