package otgviewer.client.components;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

abstract public class ImageClickCell extends AbstractCell<String> {
	private ImageResource image;

	public ImageClickCell(ImageResource image) {
		super("click");
		this.image = image;		
	}
	
	public void render(Cell.Context context, String data, SafeHtmlBuilder sb) {		
		sb.append(SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(image).getHTML()));
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
