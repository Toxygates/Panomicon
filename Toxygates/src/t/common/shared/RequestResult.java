package t.common.shared;

import java.io.Serializable;

/**
 * Represents response to a request, where we may not receive all the items we asked for.
 *
 * @param <Entity> the type of objects returned as a result of the request
 */
@SuppressWarnings("serial")
public class RequestResult<Entity> implements Serializable {
  private Entity[] items;
  private int totalCount;

  public RequestResult() {}

  public RequestResult(Entity[] entities, int total) {
    items = entities;
    totalCount = total;
  }

  public Entity[] items() {
    return items;
  }

  public int totalCount() {
    return totalCount;
  }
}
