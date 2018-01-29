package actors;

/**
 * Specialization of context sent when dispatching a message.
 */
public interface MessageContext extends Context {
  Actor getSender();
}
