package actors.receivers;

import actors.MessageContext;
import actors.ReceiveException;
import actors.Receiver;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class MatchingReceiver implements Receiver {
  private final List<OptionalReceiver> receivers;

  public MatchingReceiver(final List<OptionalReceiver> receivers) {
    this.receivers = new ArrayList<>(Objects.requireNonNull(receivers, "receivers cannot be null"));
  }

  @Override
  public Receiver receive(Object message, MessageContext context) throws Exception {
    System.out.println("Receiving: " + receivers);
    return receivers.stream()
        .map(r -> wrapExceptions(() -> r.receive(message, context)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .orElse(context.getReceiver());
  }

  private static <T> T wrapExceptions(final Callable<T> operation) {
    try {
      return operation.call();
    } catch (ReceiveException e) {
      throw e;
    } catch (Exception e) {
      throw new ReceiveException(e);
    }
  }

  public static class Builder {
    private final List<OptionalReceiver> receivers = new ArrayList<>();

    public <C> Builder match(final Class<C> cls, final Predicate<C> predicate, final ReceiveMessage<C> receiver) {
      receivers.add((msg, context) -> {
        System.out.println("Matching: " + cls + ", " + msg.getClass());
        if (cls.isAssignableFrom(msg.getClass()) && predicate.test((C) msg)) {
          return Optional.of(receiver.receive((C) msg, context));
        }
        return Optional.empty();
      });
      return this;
    }

    public <C> Builder match(final Class<C> cls, final Predicate<C> predicate, final ReceiveMessageVoid<C> receiver) {
      return match(cls, predicate, (msg, context) -> {
        receiver.receive(msg, context);
        return context.getReceiver();
      });
    }

    public <C> Builder match(final Class<C> cls, final ReceiveMessage<C> receiver) {
      return match(cls, msg -> true, receiver);
    }

    public <C> Builder match(final Class<C> cls, final ReceiveMessageVoid<C> receiver) {
      return match(cls, msg -> true, receiver);
    }

    public <C> Builder equals(final C obj, final ReceiveMessage<C> receiver) {
      receivers.add((msg, context) -> {
        if (msg.equals(obj)) {
          return Optional.of(receiver.receive((C) msg, context));
        }
        return Optional.empty();
      });
      return this;
    }

    public <C> Builder equals(final C obj, final ReceiveMessageVoid<C> receiver) {
      return equals(obj, (msg, context) -> {
        receiver.receive(msg, context);
        return context.getReceiver();
      });
    }

    public MatchingReceiver build() {
      return new MatchingReceiver(receivers);
    }
  }

  @FunctionalInterface
  public interface ReceiveMessage<C> {
    Receiver receive(C msg, MessageContext context) throws Exception;
  }

  @FunctionalInterface
  public interface ReceiveMessageVoid<C> {
    void receive(C msg, MessageContext context) throws Exception;
  }
}
