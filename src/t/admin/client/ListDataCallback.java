package t.admin.client;

import java.util.Arrays;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.ListDataProvider;

public class ListDataCallback<T> implements AsyncCallback<T[]> {

	final ListDataProvider<T> provider;
	final String description;
	
	public ListDataCallback(ListDataProvider<T> provider, String description) {
		this.provider = provider;
		this.description = description;
	}

	@Override
	public void onFailure(Throwable caught) {
		Window.alert("Unable to obtain " + description + " from server.");
		
	}

	@Override
	public void onSuccess(T[] result) {
		provider.setList(Arrays.asList(result));		
	}

}
