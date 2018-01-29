package actors.receivers;

import actors.MessageContext;
import actors.Receiver;
import java.util.Optional;

@FunctionalInterface
public interface OptionalReceiver {
  Optional<Receiver> receive(Object message, MessageContext context) throws Exception;
}
