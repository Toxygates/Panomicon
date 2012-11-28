package otgviewer.client.components;

import com.google.gwt.user.client.rpc.*;

public abstract class PendingAsyncCallback<T> implements AsyncCallback<T> {

	private DataListenerWidget widget;
	public PendingAsyncCallback(DataListenerWidget _widget) {
		widget = _widget;
		widget.addPendingRequest();
	}
	
	public void onSuccess(T t) {
		handleSuccess(t);
		widget.removePendingRequest();
	}
	
	abstract public void handleSuccess(T t);
	
	public void onFailure(Throwable caught) {
		handleFailure(caught);
		widget.removePendingRequest();
	}
	
	abstract public void handleFailure(Throwable caught);
}
