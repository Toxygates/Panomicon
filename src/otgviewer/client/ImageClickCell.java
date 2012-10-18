package otgviewer.client;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

abstract public class ImageClickCell extends AbstractCell<String> {
	private String image;

	public ImageClickCell(String image) {
		super("click");
		this.image = image;
	}
	
	public void render(Cell.Context context, String data, SafeHtmlBuilder sb) {
		sb.appendHtmlConstant("<img src=\"images/" + image + "\">");
	}
	

	@Override
	public void onBrowserEvent(Context context, Element parent, String value,
			NativeEvent event, ValueUpdater<String> valueUpdater) {
		if ("click".equals(event.getType())) {
			onClick(value);
		} else {
			super.onBrowserEvent(context, parent, value, event, valueUpdater);
		}
	}

	abstract public void onClick(String value); 
	
}
