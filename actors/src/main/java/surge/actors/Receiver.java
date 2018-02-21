package surge.actors;

import java.util.Optional;
import surge.actors.receivers.MatchingReceiver.Builder;

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

  static Builder builder() {
    return new Builder();
  }
}
