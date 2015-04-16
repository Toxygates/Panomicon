package otgviewer.client.components;

import java.util.logging.Level;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class PendingAsyncCallback<T> implements AsyncCallback<T> {

	private DataListenerWidget widget;
	private String onErrorMessage;
	public PendingAsyncCallback(DataListenerWidget _widget, String _onErrorMessage) {
		widget = _widget;
		onErrorMessage = _onErrorMessage;
		widget.addPendingRequest();		
	}
	
	public PendingAsyncCallback(DataListenerWidget _widget) {
		this(_widget, "There was a server-side error.");
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
	
	public void handleFailure(Throwable caught) {
		Window.alert(onErrorMessage + ":" + caught.getMessage());
		widget.getLogger().log(Level.SEVERE, onErrorMessage, caught);
	}
	
}
