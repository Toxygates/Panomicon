package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import bioweb.shared.SharedUtils;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.StackLayoutPanel;
import com.google.gwt.user.client.ui.TextArea;

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
		protected StackedListEditor stackedEditor;
		protected LayoutPanel p = new LayoutPanel();
//		protected VerticalPanel vp = Utils.mkVerticalPanel();
		
		/**
		 * @param stackedEditor The editor that this selection method belongs to.
		 */
		public SelectionMethod(StackedListEditor stackedEditor) {
			this.stackedEditor = stackedEditor;
			initWidget(p);
			p.setWidth("100%");
			p.setHeight("100%");
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

	}
	
	/**
	 * A selection method that allows the user to edit a list as text, freely.
	 * Items are separated by commas or whitespace.
	 */
	public static class FreeEdit extends SelectionMethod {
		protected TextArea textArea = new ResizableTextArea();
		public FreeEdit(StackedListEditor editor) {
			super(editor);			
			p.add(textArea);
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
			final BrowseCheck bc = this;
			this.selTable = new StringSelectionTable("Sel.", itemTitle) {
				protected void selectionChanged(Set<String> selected) {
					stackedEditor.setSelection(selected, bc);					
				}
			};
//			selTable.setSize("100%", "100px");
			
			p.add(new ScrollPanel(selTable));
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
		StackLayoutPanel slp = new StackLayoutPanel(Unit.EM);
		initWidget(slp);
		createSelectionMethods(methods, itemTitle);
		for (SelectionMethod m: methods) {
//			slp.add(m, m.getTitle());
			slp.add(m, m.getTitle(), 2.2);
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
	
	/**
	 * Change the selection.
	 * @param items New selection
	 * @param from The selection method that triggered the change, or null 
	 * if the change was triggered externally.
	 */
	protected void setSelection(Collection<String> items, 
			@Nullable SelectionMethod from) {
		for (SelectionMethod m: methods) {
			if (m != from) {
				m.setSelection(items);
			}
		}
		selectedItems = new HashSet<String>(items);
		selectionChanged(selectedItems);
	}
	
	public void setSelection(Collection<String> items) {
		setSelection(items, null);		
	}
	
	/**
	 * Outgoing signal. Called when the selection has changed.
	 * @param items
	 */
	protected void selectionChanged(Set<String> items) {}
	
	public void clearSelection() {
		setSelection(new HashSet<String>());
	}

}
