package otgviewer.client.components;

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
	
	protected abstract T[] values();
}
