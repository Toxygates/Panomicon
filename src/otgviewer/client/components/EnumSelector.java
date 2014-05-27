package otgviewer.client.components;

import java.util.Arrays;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;

/**
 * A convenience widget for selecting among the values of an enum.
 * NB this assumes that the toString values of each enum member
 * are unique.
 * @author johan
 *
 * @param <T>
 */
public abstract class EnumSelector<T extends Enum<T>> extends Composite {

	private ListBox lb = new ListBox();
	public EnumSelector() {
		initWidget(lb);
		lb.setVisibleItemCount(1);
		for (T t: values()) {
			lb.addItem(t.toString());
		}		
		lb.addChangeHandler(new ChangeHandler() {			
			@Override
			public void onChange(ChangeEvent event) {				
				onValueChange(value());			
			}  
		});		
	}
	
	public ListBox listBox() {
		return lb;
	}
	
	private T parse(String s) {
		for (T t: values()) {
			if (s.equals(t.toString())) {
				return t;
			}
		}
		return null;
	}
	
	public T value() {
		if (lb.getSelectedIndex() == -1) {
			return null;
		}
		return parse(lb.getItemText(lb.getSelectedIndex()));
	}
	
	public void reset() {
		lb.setSelectedIndex(0);
	}
	
	protected abstract T[] values();
	
	public void setSelected(T t) {
		int idx = Arrays.binarySearch(values(), t);
		if (idx != -1) {
			lb.setSelectedIndex(idx);
		}
	}
	
	protected void onValueChange(T selected) { }
}
