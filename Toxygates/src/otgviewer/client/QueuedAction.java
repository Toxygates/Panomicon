package otgviewer.client;

/**
 * An action to be invoked at some later time (for example when data becomes
 * available)
 * 
 * @author johan
 *
 */
public abstract class QueuedAction implements Runnable {
  String name;

  public QueuedAction(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof QueuedAction) {
      return name.equals(((QueuedAction) other).name);
    }
    return false;
  }

  @Override
  abstract public void run();
}

