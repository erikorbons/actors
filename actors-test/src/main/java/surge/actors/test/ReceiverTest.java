package surge.actors.test;

import java.util.Objects;
import surge.actors.Actor;
import surge.actors.MessageContext;
import surge.actors.Receiver;

public class ReceiverTest {
  private final ProbeActor self;
  private Receiver currentReceiver;
  private final TestScheduler scheduler;
  private final TestContext context;

  private ReceiverTest(final ProbeActor self, final TestScheduler scheduler,
      final Receiver currentReceiver) {
    this.self = Objects.requireNonNull(self, "self cannot be null");
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler cannot be null");
    this.currentReceiver = Objects
        .requireNonNull(currentReceiver, "currentReceiver cannot be null");

    this.context = new TestContext(this, scheduler, self, null);
  }

  public static ReceiverTest of(final Receiver currentReceiver) {
    return new ReceiverTest(new ProbeActor(), new TestScheduler(), currentReceiver);
  }

  public Receiver getCurrentReceiver() {
    return currentReceiver;
  }

  public void receive(final Object message, final Actor sender) throws Exception {
    final MessageContext messageContext = new MessageContextTest(context, sender);

    currentReceiver = currentReceiver
        .receive(message, messageContext)
        .orElse(currentReceiver);
  }
}
