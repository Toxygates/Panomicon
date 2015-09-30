package otgviewer.client.components;

import java.util.List;

import com.google.gwt.event.shared.EventHandler;

public interface ExactMatchHandler<T> extends EventHandler {

  public abstract void onExactMatchFound(List<T> exactMatches);

}
