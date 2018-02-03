package actors;

import actors.receivers.MatchingReceiver;
import java.util.Optional;

@FunctionalInterface
public interface Receiver {
  Optional<Receiver> receive(Object message, MessageContext context) throws Exception;

  static MatchingReceiver.Builder builder() {
    return new MatchingReceiver.Builder();
  }
}
