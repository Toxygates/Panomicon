package otgviewer.client;

import javax.annotation.Nullable;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A dialog for displaying and modifying a column filter.
 * @author johan
 */
public class FilterEditor extends Composite {

	private TextBox input = new TextBox();
	NumberFormat fmt = NumberFormat.getScientificFormat();
	public FilterEditor(String column, boolean isUpper, @Nullable Double initValue) {
		VerticalPanel vp = Utils.mkVerticalPanel(true);
		initWidget(vp);
		vp.setWidth("300px");
		
		Label l = new Label("Please choose a bound for the column '" + column + "'. Examples: " +
				fmt.format(0.3) + ", " + fmt.format(1.2e-3));
		l.setWordWrap(true);
		vp.add(l);
		
		if (initValue != null) {
			input.setValue(fmt.format(initValue));
		}
		
		Label l1 = new Label(isUpper ? "x >=" : "x <=");
		HorizontalPanel hp = Utils.mkHorizontalPanel(true, l1, input);
		vp.add(hp);
		
		Button setButton = new Button("OK");
		Button clearButton = new Button("Clear filter");
		hp = Utils.mkHorizontalPanel(true, setButton, clearButton);
		vp.add(hp);
	}

}
