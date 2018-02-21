package actors;

import java.util.Optional;

public interface Message {
  Object getPayload();
  Actor getSender();
  PublishMode getPublishMode();
  Optional<Filter> getPublishFilter();

  enum PublishMode {
    LOCAL_AND_PUBLISH,
    PUBLISH_ONLY
  }
}
