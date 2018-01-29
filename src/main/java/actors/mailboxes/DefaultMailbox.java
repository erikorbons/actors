package actors.mailboxes;

import actors.Context;
import actors.Mailbox;
import actors.Message;
import actors.PrivateContext;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultMailbox implements Mailbox {

  private final static int STATUS_SCHEDULED = 1;

  private final PrivateContext context;
  private final ConcurrentLinkedQueue<Message> queue = new ConcurrentLinkedQueue<>();
  private final AtomicInteger status = new AtomicInteger(0);

  public DefaultMailbox(final PrivateContext context) {
    this.context = Objects.requireNonNull(context, "context cannot be null");
  }

  @Override
  public void enqueue(final Message message) {
    queue.add(message);

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

      if (status.compareAndSet(currentStatus, currentStatus & STATUS_SCHEDULED)) {
        // We changed the status to scheduled, now schedule mailbox delivery:
        break;
      }
    }

    // Make the dispatcher schedule the mailbox:
    context.getScheduler().schedule(this::drain);
  }

  private void drain() {
    Message message;

    while ((message = queue.poll()) != null) {
      context.dispatchMessage(message);
    }

    // Reset the status:
    int currentStatus;
    do {
      currentStatus = status.get();
    } while (!status.compareAndSet(currentStatus, currentStatus & (~STATUS_SCHEDULED)));

    // Schedule delivery if the queue isn't empty:
    if (!queue.isEmpty()) {
      scheduleDelivery();
    }
  }
}
