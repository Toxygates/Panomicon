package otgviewer.client.dialog;

import javax.annotation.Nullable;

import otgviewer.client.Utils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class InputDialog extends Composite {

	public InputDialog(String message) {
		VerticalPanel vp = new VerticalPanel();
		vp.setWidth("100%");
		vp.add(new Label(message));
		initWidget(vp);
		
		final TextBox input = new TextBox();
		vp.add(input);
		
		Button b = new Button("OK");
		b.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				onChange(input.getText());
				
			}
		});
		
		Button b2 = new Button("Cancel");
		b2.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				onChange(null);			
			}
		});		
		HorizontalPanel hp = Utils.mkHorizontalPanel(true, b, b2);
		hp.setWidth("100%");				
		vp.add(hp);
	}
	
	protected void onChange(@Nullable String result) {
		
	}

}
