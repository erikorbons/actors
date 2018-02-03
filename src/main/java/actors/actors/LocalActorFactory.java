package actors.actors;

import actors.ActorFactory;
import actors.Mailbox;
import actors.Scheduler;
import actors.mailboxes.DefaultMailbox;
import java.util.Objects;

public class LocalActorFactory implements ActorFactory {
  private final Scheduler scheduler;

  public LocalActorFactory(final Scheduler scheduler) {
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler cannot be null");
  }

  public Scheduler getScheduler() {
    return scheduler;
  }

  @Override
  public LocalActor spawn(final FactoryWithContext entry, final String name) {
    Objects.requireNonNull(entry, "entry cannot be null");
    Objects.requireNonNull(name, "name cannot be null");

    // Create a blank context for the actor, with initial state and without a
    // parent actor.
    final LocalActorContext context = new LocalActorContext(null, scheduler);

    // Create a mailbox that dispatches to the previously created actor context:
    final Mailbox mailbox = new DefaultMailbox(context);

    // Create an actor handler that dispatches messages to the previously created
    // mailbox:
    final LocalActor actor = new LocalActor(name, mailbox);

    // Initialize the context with the newly created actor:
    context.initialize(actor, entry);

    return actor;
  }
}