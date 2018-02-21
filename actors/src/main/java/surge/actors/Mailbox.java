package surge.actors;

public interface Mailbox {

  void enqueue(Message message);
  void enqueueSystemMessage(Message message);
}
