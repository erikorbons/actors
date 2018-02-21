package surge.actors;

@FunctionalInterface
public interface ActorFactory {
  Actor spawn(FactoryWithContext entry, String name);

  default Actor spawn(Factory entry, String name) {
    return spawn(ctx -> entry.apply(), name);
  }

  @FunctionalInterface
  interface Factory {
    Receiver apply();
  }

  @FunctionalInterface
  interface FactoryWithContext {
    Receiver apply(Context context);
  }
}
