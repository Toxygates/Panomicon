package t.common.client;

/**
 * Callback interface for an acceptor of an asynchronously produced value.
 * @param <T>
 */
public interface ValueAcceptor<T> {
  void acceptValue(T value);
}
