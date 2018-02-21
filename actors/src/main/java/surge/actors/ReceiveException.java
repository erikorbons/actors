package surge.actors;

public class ReceiveException extends RuntimeException {
  public ReceiveException(final Throwable cause) {
    super(cause);
  }
}
