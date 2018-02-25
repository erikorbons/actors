package surge.actors.bridge.actors;

import java.util.HashMap;
import java.util.Optional;
import surge.actors.Actor;
import surge.actors.Filter;
import surge.actors.Path;
import surge.actors.Receiver;
import surge.actors.bridge.messages.ExternalMessage;
import surge.actors.messages.ActorTerminated;
import surge.actors.messages.Ping;
import surge.actors.messages.PingResponse;
import surge.actors.messages.Watching;

public abstract class MessageDispatcher {

  public static Receiver forActor(final Actor mainActor) {
    final HashMap<Path, Actor> actors = new HashMap<>();

    return Receiver.builder()
        .match(ExternalMessage.class, (msg, ctx) -> {
          final Path destination = msg.getDestination();

          // Send directly to the actor if the actor is known, otherwise transmit
          // the message by publishing to the given path and locate the actor
          // for future reference.
          Optional.ofNullable(actors.get(destination))
              .ifPresentOrElse(
                  target -> target.tell(msg.getPayload(), msg.getSender()),
                  () -> {
                    final Filter filter = Filter.fromPath(destination);

                    mainActor.publish(filter, msg.getPayload(), msg.getSender());
                    mainActor.publish(filter, new Ping(), ctx.getSelf());
                  }
              );
        })
        .match(PingResponse.class, (pingResponse, ctx) -> {
          ctx.watch(ctx.getSender());
          actors.put(ctx.getSender().getPath(), ctx.getSender());
        })
        .match(ActorTerminated.class, (terminated, ctx) -> {
          actors.remove(terminated.getActor().getPath());
        })
        .build();
  }
}
