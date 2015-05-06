package t.admin.client;

import java.util.Date;

import t.common.shared.DataRecord;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;

public class StandardColumns<T extends DataRecord> {

	final protected CellTable<T> table;
	StandardColumns(CellTable<T> table) {
		this.table = table;
	}
	
	void addStartColumns() {
		TextColumn<T> textColumn = new TextColumn<T>() {
			@Override
			public String getValue(T object) {
				return object.getTitle();
			}
		};				
		table.addColumn(textColumn, "ID");
		table.setColumnWidth(textColumn, "12.5em");
		
		TextColumn<T> commentColumn = new TextColumn<T>() {
			@Override
			public String getValue(T object) {
				return object.getComment();				
			}
		};
		table.addColumn(commentColumn, "Comment");
		
		TextColumn<T> dateColumn = new TextColumn<T>() {
			@Override
			public String getValue(T object) {
				Date d = object.getDate();
				if (d != null) {
					String time = DateTimeFormat.getFormat(
							PredefinedFormat.DATE_TIME_SHORT).format(d);
					return time;
				} else {
					return "N/A";
				}
			}
		};
		table.addColumn(dateColumn, "Added");
	}
	
	void addDeleteColumn() {
		ButtonCell deleteCell = new ButtonCell();		
		Column<T, String> deleteColumn = new Column<T, String>(deleteCell) {
			public String getValue(T b) {
				return "Delete";
			}
		};
		table.addColumn(deleteColumn);
		deleteColumn.setFieldUpdater(new FieldUpdater<T, String>() {
			@Override
			public void update(int index, T object, String value) {
				onDelete(object);				
			}			
		});	
	}
	
	void onDelete(T object) {
		
	}
	
}
