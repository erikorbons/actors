package actors;

public interface Mailbox {

  void enqueue(Message message);
}
