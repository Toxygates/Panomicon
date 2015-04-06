package t.common.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import t.common.client.components.SelectionTable;
import t.common.shared.DataRecord;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import static t.common.client.Utils.makeButtons;

/**
 * Dialog for selecting from a list of DataRecords.
 * @param <T> the type of DataRecord being selected.
 */
public class DataRecordSelector<T extends DataRecord> extends Composite {

	protected SelectionTable<T> st;
	protected VerticalPanel vp;
	
	public DataRecordSelector(Collection<T> items, String message) {
		vp = new VerticalPanel();
		initWidget(vp);

		Label l = new Label(message);
		vp.add(l);
		
		st = new SelectionTable<T>("", true) {
			@Override
			protected void initTable(CellTable<T> table) {
				TextColumn<T> tc = new TextColumn<T>() {
					@Override
					public String getValue(T object) {
						return object.getUserTitle();
					}
				};
				table.addColumn(tc, "Title");				
			}
		};
		setItems(items);
		
		List<Command> commands = new ArrayList<Command>();
		commands.add(new Command("OK") {
			@Override 
			public void run() { onOK(); }
		});
		commands.add(new Command("Cancel") {
			@Override 
			public void run() { onCancel(); }
		});
		
		vp.add(st);
		vp.add(makeButtons(commands));	
	}

	public Set<T> getSelection() {
		return st.getSelection();
	}
	
	public void setItems(Collection<T> data) {
		st.setItems(new ArrayList<T>(data));
	}
	
	public void setSelection(Collection<T> items) {
		st.setSelection(items);
	}
	
	public void selectAll() {
		st.selectAll(st.getItems());
	}
	
	public void onOK() {
		
	}
	
	public void onCancel() {
		
	}
}
