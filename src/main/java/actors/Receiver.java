package actors;

import actors.receivers.MatchingReceiver;

@FunctionalInterface
public interface Receiver {
  Receiver receive(Object message, MessageContext context) throws Exception;

  static MatchingReceiver.Builder builder() {
    return new MatchingReceiver.Builder();
  }
}
