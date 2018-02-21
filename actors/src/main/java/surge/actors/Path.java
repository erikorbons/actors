package surge.actors;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Path {
  String getName();
  Optional<Path> getParent();

  static Path of(final String name) {
    return new RootPath(Objects.requireNonNull(name, "name cannot be null"));
  }

  static Path of(final Path parent, final String name) {
    return new SubPath(
        Objects.requireNonNull(parent, "parent cannot be null"),
        Objects.requireNonNull(name, "name cannot be null")
    );
  }

  default Stream<String> getElements() {
    return Stream.concat(
        getParent()
            .map(Path::getElements)
            .orElse(Stream.empty()),
        Stream.of(getName()));
  }

  default String getFullName() {
    return getElements().collect(Collectors.joining("/"));
  }

  final class RootPath implements Path {
    private final String name;

    public RootPath(final String name) {
      this.name = Objects.requireNonNull(name, "name cannot be null");

      if (name.contains("/")) {
        throw new IllegalArgumentException("Actor names must not contain /");
      }
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Optional<Path> getParent() {
      return Optional.empty();
    }

    @Override
    public String toString() {
      return getFullName();
    }
  }

  final class SubPath implements Path {
    private final Path parent;
    private final String name;

    public SubPath(final Path parent, final String name) {
      this.parent = Objects.requireNonNull(parent, "parent cannot be null");
      this.name = Objects.requireNonNull(name, "name cannot be null");

      if (name.contains("/")) {
        throw new IllegalArgumentException("Actor names must not contain /");
      }
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Optional<Path> getParent() {
      return Optional.of(parent);
    }

    @Override
    public String toString() {
      return getFullName();
    }
  }
}
