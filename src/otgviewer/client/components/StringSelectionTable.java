package otgviewer.client.components;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;

public class StringSelectionTable extends SelectionTable<String> {

	String _title;
	public StringSelectionTable(String selectColTitle, String title) {
		super(selectColTitle, true);
		_title = title;
	}
	
	protected void initTable(CellTable<String> table) {
		TextColumn<String> textColumn = new TextColumn<String>() {
			@Override
			public String getValue(String object) {
				return object;
			}
		};
		table.addColumn(textColumn, _title);
		table.setColumnWidth(textColumn, "12.5em");
	}
}
