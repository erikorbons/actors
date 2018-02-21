package surge.actors;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Filter {

  String getLevel();
  Optional<Filter> getRemainder();

  default boolean isEmpty() {
    return getLevel().isEmpty() && !getRemainder().isPresent();
  }

  default Stream<String> getLevels() {
    return Stream.concat(
        Stream.of(getLevel()),
        getRemainder().map(Filter::getLevels).orElse(Stream.empty())
    );
  }

  default String getFullFilter() {
    return getLevels().collect(Collectors.joining("/"));
  }

  static Filter empty() {
    return new TerminalFilter("");
  }

  static Filter of(final String filter) {
    Objects.requireNonNull(filter, "filter cannot be null");

    final String[] parts = filter.split("/+");

    Filter currentFilter = new TerminalFilter(parts[parts.length - 1]);

    for (int i = parts.length - 2; i >= 0; -- i) {
      currentFilter = new NonTerminalFilter(parts[i], currentFilter);
    }

    return currentFilter;
  }

  class TerminalFilter implements Filter {
    private final String level;

    public TerminalFilter(final String level) {
      this.level = Objects.requireNonNull(level, "level cannot be null");

      if (level.length() > 1) {
        if (level.contains("#")) {
          throw new IllegalArgumentException("The # character must occupy an entire filter level");
        }
        if (level.contains("+")) {
          throw new IllegalArgumentException("The + character must occupy an entire filter level");
        }
      }
    }

    @Override
    public String getLevel() {
      return level;
    }

    @Override
    public Optional<Filter> getRemainder() {
      return Optional.empty();
    }
  }

  class NonTerminalFilter implements Filter {
    private final String level;
    private final Filter remainder;

    public NonTerminalFilter(final String level, final Filter remainder) {
      this.level = Objects.requireNonNull(level, "level cannot be null");
      this.remainder = Objects.requireNonNull(remainder, "remainder cannot be null");

      if (level.contains("#")) {
        throw new IllegalArgumentException(
            "The # character must be the only character in the last filter level");
      }
      if (level.length() > 1 && level.contains("+")) {
        throw new IllegalArgumentException("The + character must occupy an entire filter level");
      }
    }

    @Override
    public String getLevel() {
      return level;
    }

    @Override
    public Optional<Filter> getRemainder() {
      return Optional.of(remainder);
    }
  }
}
