package actors.receivers;

import actors.Context;
import actors.MessageContext;
import actors.ReceiveException;
import actors.Receiver;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class MatchingReceiver implements Receiver {
  private final List<OptionalReceiver> receivers;
  private final List<Function<Exception, Optional<FailureAction>>> failureHandlers;
  private final Consumer<Context> beforeRestartHandler;
  private final Consumer<Context> afterRestartHandler;

  public MatchingReceiver(final List<OptionalReceiver> receivers,
      final List<Function<Exception, Optional<FailureAction>>> failureHandlers,
      final Consumer<Context> beforeRestartHandler,
      final Consumer<Context> afterRestartHandler) {
    this.receivers = new ArrayList<>(Objects.requireNonNull(receivers, "receivers cannot be null"));
    this.failureHandlers = new ArrayList<>(Objects.requireNonNull(failureHandlers, "failureHandlers cannot be null"));
    this.beforeRestartHandler = Objects.requireNonNull(beforeRestartHandler, "beforeRestartHandler cannot be null");
    this.afterRestartHandler = Objects.requireNonNull(afterRestartHandler, "afterRestartHandler cannot be null");
  }

  @Override
  public Optional<Receiver> receive(Object message, MessageContext context) throws Exception {
    return receivers.stream()
        .map(r -> wrapExceptions(() -> r.receive(message, context)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  @Override
  public Optional<FailureAction> handleFailure(final Exception cause) {
    return failureHandlers.stream()
        .map(handler -> handler.apply(cause))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  @Override
  public void beforeRestart(final Context context) {
    beforeRestartHandler.accept(context);
  }

  @Override
  public void afterRestart(final Context context) {
    afterRestartHandler.accept(context);
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
    private final List<Function<Exception, Optional<FailureAction>>> failureHandlers = new ArrayList<>();
    private Consumer<Context> beforeRestartHandler = ctx -> { };
    private Consumer<Context> afterRestartHandler = ctx -> { };

    public <C> Builder match(final Class<C> cls, final Predicate<C> predicate, final ReceiveMessage<C> receiver) {
      receivers.add((msg, context) -> {
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

    public <C extends Exception> Builder onFailure(final Class<C> cls, final Function<C, FailureAction> failureHandler) {
      failureHandlers.add(cause -> cls.isAssignableFrom(cls)
          ? Optional.of(failureHandler.apply((C) cause))
          : Optional.empty()
      );
      return this;
    }

    public Builder onFailure(final FailureAction defaultAction) {
      failureHandlers.add(cause -> Optional.of(defaultAction));
      return this;
    }

    public Builder beforeRestart(final Consumer<Context> handler) {
      beforeRestartHandler = handler;
      return this;
    }

    public Builder afterRestart(final Consumer<Context> handler) {
      afterRestartHandler = handler;
      return this;
    }

    public MatchingReceiver build() {
      return new MatchingReceiver(receivers, failureHandlers, beforeRestartHandler, afterRestartHandler);
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
