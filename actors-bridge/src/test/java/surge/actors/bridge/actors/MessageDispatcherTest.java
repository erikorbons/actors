package surge.actors.bridge.actors;

import org.junit.Before;
import org.junit.Test;
import surge.actors.Path;
import surge.actors.bridge.messages.ExternalMessage;
import surge.actors.messages.Ping;
import surge.actors.messages.PingResponse;
import surge.actors.test.ProbeActor;
import surge.actors.test.ReceiverTest;

public class MessageDispatcherTest {

  private ProbeActor mainActor;
  private ProbeActor externalSender;
  private ProbeActor sender;

  @Before
  public void createProbes() {
    mainActor = new ProbeActor();
    externalSender = new ProbeActor();
    sender = new ProbeActor();
  }

  @Test
  public void testDispatchMessageToUnknownActor() throws Exception {
    final ReceiverTest receiver = ReceiverTest.of(MessageDispatcher.forActor(mainActor));

    final ExternalMessage msg = new ExternalMessage(Path.of(Path.of("a"), "b"), "Hello, World!",
        externalSender);

    receiver.receive(msg, sender);

    // A message should be published:
    mainActor.assertMessage(String.class);

    // An actor should be located using a Ping:
    mainActor.assertMessage(Ping.class);

    // No more messages should be sent:
    mainActor.assertNoMessages();
    externalSender.assertNoMessages();
  }

  @Test
  public void testSubscribeActor() throws Exception {
    final Path path = Path.of(Path.of("a"), "b");
    final ReceiverTest receiver = ReceiverTest.of(MessageDispatcher.forActor(mainActor));
    final ProbeActor targetActor = new ProbeActor(path);

    // Send a ping response, this should register the actor:
    receiver.receive(new PingResponse(), targetActor);

    // Now send an external message to the actor that was just registered:
    final ExternalMessage msg = new ExternalMessage(Path.of(Path.of("a"), "b"), "Hello, World!",
        externalSender);

    receiver.receive(msg, sender);

    // A message should be published directly to the target:
    targetActor.assertMessage(String.class);

    // No messages should be sent to the main actor (dispatching is done directly to
    // the target):
    mainActor.assertNoMessages();
    targetActor.assertNoMessages();
    externalSender.assertNoMessages();
  }
}
