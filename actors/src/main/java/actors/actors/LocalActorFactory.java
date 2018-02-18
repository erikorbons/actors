package actors.actors;

import actors.Actor;
import actors.ActorFactory;
import actors.Mailbox;
import actors.Message;
import actors.Path;
import actors.Scheduler;
import actors.mailboxes.DefaultMailbox;
import actors.messages.MailboxUnsuspend;
import java.util.Objects;
import java.util.Optional;

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
    return spawn(null, entry, name);
  }

  protected LocalActor spawn(final LocalActor parent, final FactoryWithContext entry, final String name) {
    Objects.requireNonNull(entry, "entry cannot be null");
    Objects.requireNonNull(name, "name cannot be null");

    // Create a blank context for the actor, with initial state and without a
    // parent actor.
    final LocalActorContext context = new LocalActorContext(parent, scheduler);

    // Create a mailbox that dispatches to the previously created actor context:
    final Mailbox mailbox = new DefaultMailbox(context);

    // Create an actor handler that dispatches messages to the previously created
    // mailbox:
    final LocalActor actor = new LocalActor(
        Optional.ofNullable(parent).map(p -> Path.of(p.getPath(), name))
            .orElseGet(() -> Path.of(name)),
        mailbox
    );

    // Initialize the context with the newly created actor:
    context.initialize(actor, entry);

    // Unsuspend the mailbox after initialization. The mailbox starts suspended to
    // prevent messages from being delivered while it is initializing:
    final MailboxUnsuspend msg = new MailboxUnsuspend();
    mailbox.enqueueSystemMessage(new Message() {
      @Override
      public Object getPayload() {
        return msg;
      }

      @Override
      public Actor getSender() {
        return actor;
      }
    });

    return actor;
  }
}
