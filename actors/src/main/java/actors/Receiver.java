package actors;

import actors.receivers.MatchingReceiver;
import java.util.Optional;

public interface Receiver {

  enum FailureAction {
    RESUME,
    RESTART,
    KILL,
    ESCALATE
  }

  Optional<Receiver> receive(Object message, MessageContext context) throws Exception;

  Optional<FailureAction> handleFailure(Exception cause);

  void beforeRestart(Context context);
  void afterRestart(Context context);

  static MatchingReceiver.Builder builder() {
    return new MatchingReceiver.Builder();
  }
}
