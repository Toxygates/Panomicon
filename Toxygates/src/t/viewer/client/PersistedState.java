package t.viewer.client;

import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * A piece of client state that can be individually stored and applied.
 * @param <T> The type of the state
 */
abstract public class PersistedState<T> {

  protected Logger logger;
  protected String storageKey;
  protected @Nullable T value = null;
  
  public PersistedState(String name, String storageKey) {
    logger = Logger.getLogger("PersistedState." + name);
    this.storageKey = storageKey;
  }
  
  public @Nullable T value() {
    return value;
  }
  
  /**
   * Serialise the state
   */
  protected @Nullable String pack(@Nullable T state) {
    if (state == null) {
      return null;
    }
    return doPack(state);
  }
  
  abstract protected @Nullable String doPack(T state);
  
  /**
   * Deserialise the state
   */
  protected @Nullable T unpack(@Nullable String state) {
    if (state == null) {
      return null;
    } 
    return doUnpack(state);    
  }
  
  abstract protected @Nullable T doUnpack(String state); 
  
  public void loadAndApply(StorageParser parser) {
    T state = unpack(parser.getItem(storageKey));
    value = state;
    apply(state);
  }
  
  public void store(StorageParser parser, @Nullable T state) {
    String sstate = pack(state);
    if (sstate != null) {
      parser.setItem(storageKey, sstate);
    } else {
      parser.clearItem(storageKey);
    }
  }
  
  /**
   * Apply the state to the client
   */
  protected abstract void apply(@Nullable T state);
  
  /**
   * Change the value of this state as a result of e.g. 
   * a user action, persisting and then applying it.
   * @param newState
   */  
  public void changeAndPersist(StorageParser parser, @Nullable T newState) {
    logger.info("Changed");
    value = newState;
    store(parser, newState);
    apply(newState);
  }
}

