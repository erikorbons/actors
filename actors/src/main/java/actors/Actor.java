package actors;

public interface Actor {
  String getName();
  void tell(Object message, Actor sender);
}
