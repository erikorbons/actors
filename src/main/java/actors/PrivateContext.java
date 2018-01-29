package actors;

/**
 * Private extension to the actor context: this context is exposed to internal
 * classes (mailbox, scheduler, etc.) and not to receivers.
 */
public interface PrivateContext extends Context {
  void dispatchMessage(Message message);
}
