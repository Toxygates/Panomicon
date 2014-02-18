package otgviewer.client.dialog;

import otgviewer.client.Utils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class TargetMineSyncDialog extends Composite {

	final TextBox userText = new TextBox();
	final TextBox passText = new PasswordTextBox();
	final CheckBox replaceCheck = new CheckBox("Replace lists with identical names");
	
	public TargetMineSyncDialog() {
		VerticalPanel vp = new VerticalPanel();
		vp.setWidth("400px");
		initWidget(vp);
		Label l = new Label("You must have a TargetMine account in order to use " +
				"this function. If you do not have one, you may create one " +
				"at http://targetmine.nibio.go.jp.");
		l.setWordWrap(true);
		vp.add(l);
		Grid g = new Grid(2, 2);
		l = new Label("Account name (e-mail address)");
		g.setWidget(0, 0, l);
		l = new Label("Password");
		g.setWidget(1, 0, l);
		g.setWidget(0, 1, userText);
		g.setWidget(1, 1, passText);
		userText.setWidth("20em");
		passText.setWidth("20em");
		vp.add(g);
		vp.add(replaceCheck);
		
		Button b = new Button("Import");
		b.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				if (userText.getValue().trim().equals("") || passText.getValue().trim().equals("")) {
					Window.alert("Please enter both your account name and password.");
				} else {
					userProceed(userText.getValue(), passText.getValue(), replaceCheck.getValue());
				}
			}
		});

		Button b2 = new Button("Cancel");
		b2.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				userCancelled();				
			}
		});
		HorizontalPanel hp = Utils.mkHorizontalPanel(true, b, b2);
		vp.add(hp);
	}
	
	protected void userProceed(String user, String pass, boolean replace) {
		
	}
	
	// TODO factor out this mechanism
	protected void userCancelled() {
		
	}

}
