package otgviewer.client.dialog;

import otgviewer.client.Utils;
import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.InputGrid;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.rpc.MatrixService;
import otgviewer.client.rpc.MatrixServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class FeedbackForm extends InteractionDialog {

	TextArea commentArea;
	InputGrid ig;
	private final MatrixServiceAsync matrixService = (MatrixServiceAsync) GWT
			.create(MatrixService.class);	
	
	public FeedbackForm(final DataListenerWidget parent) {
		super(parent);
	}
	
	protected Widget content() {
		VerticalPanel vpanel = Utils.mkVerticalPanel();
		
		Label l = new Label("We welcome comments, bug reports, questions or any kind of inquiries.\n" +
				"If you report a problem, we will try to respond to you as soon as possible.");
		l.setWordWrap(true);
		vpanel.add(l);
		l.setWidth("40em");
		
		ig = new InputGrid("Your name:", "Your e-mail address (optional):");
		vpanel.add(ig);
		
		l = new Label("Comments:");
		vpanel.add(l);
		commentArea = new TextArea();
		commentArea.setCharacterWidth(80);
		commentArea.setVisibleLines(10);
		vpanel.add(commentArea);
		
		final FeedbackForm f = this;
		
		Button b1 = new Button("Send", new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				String name = ig.getValue(0);
				String email = ig.getValue(1);
				String fb = commentArea.getText();
				matrixService.sendFeedback(name, email, fb,
						new PendingAsyncCallback<Void>(parent, "There was a problem sending your feedback.") {					
					public void handleSuccess(Void t) {
						Window.alert("Your feedback was sent.");
						userProceed();
					}			
				});								
			}
		});
		Button b2 = new Button("Cancel", cancelHandler());						
		HorizontalPanel hp = Utils.mkHorizontalPanel(true,  b1, b2);
		vpanel.add(hp);
		return vpanel;
	}
}
