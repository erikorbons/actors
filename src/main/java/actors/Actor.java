package actors;

public interface Actor {
  void tell(Object message, Actor sender);
}
