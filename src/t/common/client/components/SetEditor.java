package t.common.client.components;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A SetEditor tracks a selection and makes available a method for modifying it.
 * 
 */
public interface SetEditor<T> {
	
	/**
	 * Optionally set the available items. This clears the selection.
	 * May not be useful for all selection methods.
	 * The available items are already validated.
	 * Some edit methods may display the items in the order given, but they
	 * should all be unique.
	 * @param items
	 */
	public void setItems(List<T> items, boolean clearSelection);
	
	public abstract void setSelection(Collection<T> items);
	
	public Set<T> getSelection();	
}
