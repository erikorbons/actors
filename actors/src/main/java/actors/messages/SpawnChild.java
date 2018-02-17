package actors.messages;

import actors.ActorFactory.Factory;
import actors.ActorFactory.FactoryWithContext;
import java.util.Objects;
import java.util.Optional;

public class SpawnChild {
  private final FactoryWithContext factory;
  private final String name;

  public SpawnChild(final FactoryWithContext factory, final String name) {
    this.factory = Objects.requireNonNull(factory, "factory cannot be null");
    this.name = Objects.requireNonNull(name, "name cannot be null");
  }

  public SpawnChild(final Factory factory, final String name) {
    this(
        Optional.ofNullable(factory).map(f -> ((FactoryWithContext) ctx -> f.apply())).orElse(null),
        name);
  }

  public String getName() {
    return name;
  }

  public FactoryWithContext getFactory() {
    return factory;
  }
}
