package actors.actors;

import actors.Actor;
import actors.ActorFactory;
import actors.Mailbox;
import actors.Message;
import actors.Scheduler;
import actors.mailboxes.DefaultMailbox;
import java.util.Objects;

public class LocalActor implements Actor {
  private final Mailbox mailbox;

  public LocalActor(final Mailbox mailbox) {
    this.mailbox = mailbox;
  }

  public static ActorFactory factory(final Scheduler scheduler) {
    return factoryWithContext -> {
      // Create a blank context for the actor, with initial state and without a
      // parent actor.
      final LocalActorContext context = new LocalActorContext(null, scheduler);

      // Create a mailbox that dispatches to the previously created actor context:
      final Mailbox mailbox = new DefaultMailbox(context);

      // Create an actor handler that dispatches messages to the previously created
      // mailbox:
      final Actor actor = new LocalActor(mailbox);

      // Initialize the context with the newly created actor:
      context.initialize(actor, factoryWithContext);

      return actor;
    };
  }

  @Override
  public void tell(final Object message, final Actor sender) {
    Objects.requireNonNull(message, "message cannot be null");
    Objects.requireNonNull(sender, "sender cannot be null");

    mailbox.enqueue(new Message() {
      @Override
      public Object getPayload() {
        return message;
      }

      @Override
      public Actor getSender() {
        return sender;
      }
    });
  }
}
