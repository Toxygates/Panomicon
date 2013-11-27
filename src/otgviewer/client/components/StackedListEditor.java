package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import otgviewer.client.Utils;
import bioweb.shared.SharedUtils;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.StackLayoutPanel;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A StackedListEditor unifies multiple different methods of editing a list of strings.
 * Strings can be: compounds, genes, probes, ...
 *
 */
public class StackedListEditor extends Composite implements SetEditor<String> {

	/**
	 * A selection method is one kind of GUI that is made available for editing the list.
	 * It calls back to the StackedListEditor when the selection changes.
	 */
	public abstract static class SelectionMethod extends Composite {
		protected StackedListEditor listEditor;
		protected VerticalPanel vp = Utils.mkVerticalPanel();
		
		/**
		 * @param listEditor The editor that this selection method belongs to.
		 */
		public SelectionMethod(StackedListEditor listEditor) {
			this.listEditor = listEditor;
			initWidget(vp);
			vp.setWidth("100%");
			vp.setHeight("100%");
		}
		
		/**
		 * Get the human-readable title of this selection method.
		 */
		public abstract String getTitle();
		
		public void setItems(List<String> items, boolean clearSelection) { }
		
		/**
		 * Set the currently selected items, reflecting the selection in the GUI.
		 * This should not cause changeSelection() to be called.
		 * The items should already have been validated.
		 * @param items
		 */
		public abstract void setSelection(Collection<String> items);
//		
//		/**
//		 * Get the currently selected items.
//		 * @return
//		 */
//		public abstract String[] getSelectedItems();
	}
	
	/**
	 * A selection method that allows the user to edit a list as text, freely.
	 * Items are separated by commas or whitespace.
	 */
	public static class FreeEdit extends SelectionMethod {
		protected TextArea textArea = new TextArea();
		public FreeEdit(StackedListEditor editor) {
			super(editor);			
			vp.add(textArea);
			textArea.setWidth("100%");
			textArea.setHeight("100%");
		}
		
		public String getTitle() {
			return "Edit/paste";
		}

		@Override
		public void setSelection(Collection<String> items) {
			textArea.setText(SharedUtils.mkString(items, "\n"));			
		}
	}
	
	/**
	 * A selection method that allows the user to browse a list and check
	 * items with checkboxes.
	 * This is only recommended if the total number of available items is
	 * small (< 1000)
	 */
	public static class BrowseCheck extends SelectionMethod {
		private StringSelectionTable selTable;
		public BrowseCheck(StackedListEditor editor, String itemTitle) {
			super(editor);
			this.selTable = new StringSelectionTable("Sel.", itemTitle) {
				protected void selectionChanged(Set<String> selected) {
					listEditor.selectionChanged(selected);					
				}
			};
			vp.add(selTable);
			selTable.setWidth("100%");
			selTable.setHeight("100%");
		}
		
		public String getTitle() {
			return "Browse";
		}

		@Override
		public void setSelection(Collection<String> items) {			
			selTable.setSelection(items);
			selTable.table().redraw();
		}
		
		@Override
		public void setItems(List<String> items, boolean clearSelection) {
			selTable.setItems(items, clearSelection);
		}
	}
	
	protected List<SelectionMethod> methods = new ArrayList<SelectionMethod>();
	protected Set<String> selectedItems = new HashSet<String>();
	protected Set<String> availableItems = new HashSet<String>();
	protected StringSelectionTable selTable = null;
	
	public StackedListEditor(String itemTitle) {
		StackPanel slp = new StackPanel();
		initWidget(slp);
		createSelectionMethods(methods, itemTitle);
		for (SelectionMethod m: methods) {
			slp.add(m, m.getTitle());
			//slp.add(m, m.getTitle(), 2);
		}
	}
	
	/**
	 * Instantiates the selection methods that are to be used.
	 * @param methods list to add methods to.
	 * @return
	 */
	protected void createSelectionMethods(List<SelectionMethod> methods, String itemTitle) {		
		methods.add(new FreeEdit(this));
		BrowseCheck bc = new BrowseCheck(this, itemTitle);
		methods.add(bc);
		this.selTable = bc.selTable; 
	}
	
	/**
	 * Obtain the inner string selection table, if it exists.
	 * May be null. 
	 * TODO: improve architecture
	 */
	@Nullable
	public StringSelectionTable selTable() {
		return selTable;
	}
	
	/**
	 * See above
	 */
	@Nullable
	public CellTable<String> table() {
		if (selTable != null) {
			return selTable.table();
		}
		return null;
	}
	
	/**
	 * This method is to be called by the selection methods when the
	 * selection changes. It must not be called too frequently.
	 * @param items
	 */
	protected void selectionChanged(Collection<String> items) {
		setSelection(items);
	}
	
	/**
	 * Validate items, some of which may have been entered manually.
	 * This method may be overridden for efficiency.
	 * @param items
	 * @return Valid items.
	 */
	protected Set<String> validateItems(Collection<String> items) {
		HashSet<String> r = new HashSet<String>();
		for (String i : items) {
			if (validateItem(i)) {
				r.add(i);
			}
		}
		return r;
	}
	
	/**
	 * Validate a single item.
	 * @param item
	 * @return True iff the item is valid.
	 */
	protected boolean validateItem(String item) {
		return true;
	}
	
	public Set<String> getSelection() {
		return selectedItems;
	}
	
	/**
	 * Set the available items.
	 * @param items
	 * @return
	 */
	public void setItems(List<String> items, boolean clearSelection) {
		for (SelectionMethod m: methods) {
			m.setItems(items, clearSelection);
		}
	}
	
	public void setSelection(Collection<String> items) {
		for (SelectionMethod m: methods) {
			m.setSelection(items);
		}
		selectedItems = new HashSet(items);
	}
	
	public void clearSelection() {
		setSelection(new HashSet<String>());
	}

}
