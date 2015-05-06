package otgviewer.client.components;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;

public class InputGrid extends Composite {

	private TextBox[] inputs;
	
	public InputGrid(String... titles) {
		int i = 0;
		inputs = new TextBox[titles.length];		
		
		Panel p = new SimplePanel();
		initWidget(p);
		Grid g = new Grid(titles.length, 2);
		p.add(g);
		
		for (String t : titles) {
			inputs[i] = initTextBox(i);
			inputs[i].setWidth("20em");
			g.setWidget(i, 0, new Label(titles[i]));
			g.setWidget(i, 1, inputs[i]);						
			i += 1;
		}
	}
	
	protected TextBox initTextBox(int i) {
		return new TextBox();
	}
	
	public String getValue(int i) {
		return inputs[i].getValue();
	}
}
