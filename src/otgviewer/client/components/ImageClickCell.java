package otgviewer.client.components;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 * A clickable cell that displays an image and optionally text.
 * If text is displayed, it will appear before the image.
 */
abstract public class ImageClickCell<T> extends AbstractCell<T> {
	private ImageResource image;
	private boolean displayText;

	public ImageClickCell(ImageResource image, boolean displayText) {
		super("click");
		this.image = image;		
		this.displayText = displayText;
	}
	
	public void render(Cell.Context context, T data, SafeHtmlBuilder sb) {		
		if (displayText) {
			appendText(data, sb);
		}
		sb.append(SafeHtmlUtils.fromTrustedString("<span style=\"margin:5px\">" + 
				AbstractImagePrototype.create(image).getHTML() +
				"</span>"));
	}
	
	abstract protected void appendText(T text, SafeHtmlBuilder sb);

	@Override
	public void onBrowserEvent(Context context, Element parent, T value,
			NativeEvent event, ValueUpdater<T> valueUpdater) {		
		if ("click".equals(event.getType())) {			
			EventTarget et = event.getEventTarget();
			if (Element.is(et)) {
				Element e = et.cast();
				String target = e.getString();

				// TODO this is a bit hacky - is there a better way?
				boolean targetWasImage = (target.startsWith("<img") | target.startsWith("<IMG"));
				if (targetWasImage) {
					onClick(value);
					return;
				}
			}
		} 
		super.onBrowserEvent(context, parent, value, event, valueUpdater);		
	}

	abstract public void onClick(T value);
	
	abstract public static class StringImageClickCell extends
			ImageClickCell<String> {
		public StringImageClickCell(ImageResource image, boolean displayText) {
			super(image, displayText);
		}

		protected void appendText(String text, SafeHtmlBuilder sb) {
			sb.appendHtmlConstant(text);
		}
	}
	
	abstract public static class SafeHtmlImageClickCell extends
			ImageClickCell<SafeHtml> {
		public SafeHtmlImageClickCell(ImageResource image, boolean displayText) {
			super(image, displayText);
		}

		protected void appendText(SafeHtml text, SafeHtmlBuilder sb) {
			sb.append(text);
		}
	}
}

