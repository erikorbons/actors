package actors;

@FunctionalInterface
public interface ActorFactory {
  Actor spawn(FactoryWithContext entry);

  default Actor spawn(Factory entry) {
    return spawn(ctx -> entry.apply());
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
