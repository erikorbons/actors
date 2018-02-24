package surge.actors.mailboxes;

import surge.actors.Mailbox;
import surge.actors.Message;
import surge.actors.PrivateContext;
import surge.actors.messages.MailboxSuspend;
import surge.actors.messages.MailboxTerminate;
import surge.actors.messages.MailboxUnsuspend;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultMailbox implements Mailbox {

  private final static int STATUS_SCHEDULED = 1;
  private final static int STATUS_SUSPENDED = 2;
  private final static int STATUS_TERMINATED = 4;

  private final PrivateContext context;
  private final ConcurrentLinkedQueue<Message> systemQueue = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Message> queue = new ConcurrentLinkedQueue<>();
  private final AtomicInteger status = new AtomicInteger(STATUS_SUSPENDED);

  public DefaultMailbox(final PrivateContext context) {
    this.context = Objects.requireNonNull(context, "context cannot be null");
  }

  @Override
  public void enqueue(final Message message) {
    if ((status.get() & STATUS_TERMINATED) != 0) {
      return;
    }

    queue.add(message);

    // Schedule delivery:
    scheduleDelivery();
  }

  @Override
  public void enqueueSystemMessage(final Message message) {
    systemQueue.add(message);

    // Schedule delivery:
    scheduleDelivery();
  }

  private void scheduleDelivery() {
    while (true) {
      final int currentStatus = status.get();

      // See if the queue is scheduled:
      if ((currentStatus & STATUS_SCHEDULED) == STATUS_SCHEDULED) {
        // Already scheduled, return:
        return;
      }

      if (status.compareAndSet(currentStatus, currentStatus | STATUS_SCHEDULED)) {
        // We changed the status to scheduled, now schedule mailbox delivery:
        break;
      }
    }

    // Make the dispatcher schedule the mailbox:
    context.getScheduler().schedule(this::drain);
  }

  private void drain() {
    Message message;

    if ((status.get() & STATUS_TERMINATED) != 0) {
      return;
    }

    // Dispatch scheduled system message before any "normal" messages:
    while ((message = systemQueue.poll()) != null) {
      final Object payload = message.getPayload();

      if (payload instanceof MailboxSuspend) {
        // Set the suspended flag on the mailbox:
        while (true) {
          final int currentStatus = status.get();
          if (status.compareAndSet(currentStatus, currentStatus | STATUS_SUSPENDED)) {
            break;
          }
        }
      } else if (payload instanceof MailboxUnsuspend) {
        // Clear the suspended flag on the mailbox:
        while (true) {
          final int currentStatus = status.get();
          if (status.compareAndSet(currentStatus, currentStatus & (~STATUS_SUSPENDED))) {
            break;
          }
        }
      } else if (payload instanceof MailboxTerminate) {
        while (true) {
          final int currentStatus = status.get();
          if (status.compareAndSet(currentStatus, currentStatus & (~STATUS_TERMINATED))) {
            break;
          }
        }
        queue.clear();
      } else {
        context.dispatchMessage(message);
      }
    }

    // Process non-system messages only if the mailbox is not suspended:
    if ((status.get() & (STATUS_SUSPENDED | STATUS_TERMINATED)) == 0) {
      while ((message = queue.poll()) != null) {
        if (!context.dispatchMessage(message)) {
          // Stop dispatching if dispatching the message fails. This defers further
          // message processing and allows system messages to take precedence, which
          // may now suspend the mailbox.
          break;
        }
      }
    }

    // Reset the status:
    int currentStatus;
    do {
      currentStatus = status.get();
    } while (!status.compareAndSet(currentStatus, currentStatus & (~STATUS_SCHEDULED)));

    // Schedule delivery if the queue isn't empty:
    if (!systemQueue.isEmpty()) {
      scheduleDelivery();
    }
    if (!queue.isEmpty() && (status.get() & STATUS_SUSPENDED) != STATUS_SUSPENDED) {
      // Re-schedule delivery only if the queue has items and if the mailbox is not
      // currently suspended.
      scheduleDelivery();
    }
  }
}
