package actors;

public interface Message {
  Object getPayload();
  Actor getSender();
}
