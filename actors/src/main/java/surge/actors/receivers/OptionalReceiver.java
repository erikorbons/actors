package surge.actors.receivers;

import surge.actors.MessageContext;
import surge.actors.Receiver;
import java.util.Optional;

@FunctionalInterface
public interface OptionalReceiver {
  Optional<Receiver> receive(Object message, MessageContext context) throws Exception;
}
