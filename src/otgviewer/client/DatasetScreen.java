package otgviewer.client;

import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;
import otgviewer.shared.ValueType;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DatasetScreen extends Screen {

	ListBox valueTypeList = new ListBox();
	static String key = "ds";
	
	public DatasetScreen(Screen parent) {
		super(parent, "Dataset selection", key);		
	}
	
	public Widget content() {
		VerticalPanel hp = new VerticalPanel();
		hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		hp.setWidth("250px");
		
		hp.add(new Label("Value type"));
		valueTypeList.addItem(ValueType.Absolute.toString());
		valueTypeList.addItem(ValueType.Folds.toString());
		valueTypeList.setVisibleItemCount(1);
		hp.add(valueTypeList);
		
		hp.add(new Label("Data set"));
		
		hp.add(datasetButton("Human, in vitro", new DataFilter(CellType.Vitro,
				Organ.Kidney, RepeatType.Single, Organism.Human)));
		hp.add(datasetButton("Rat, in vitro", new DataFilter(CellType.Vitro,
				Organ.Kidney, RepeatType.Single, Organism.Rat)));
		hp.add(datasetButton("Rat, in vitro, liver, single", new DataFilter(
				CellType.Vivo, Organ.Liver, RepeatType.Single, Organism.Rat)));
		hp.add(datasetButton("Rat, in vivo, liver, repeat", new DataFilter(
				CellType.Vivo, Organ.Liver, RepeatType.Repeat, Organism.Rat)));
		hp.add(datasetButton("Rat, in vivo, kidney, single", new DataFilter(
				CellType.Vivo, Organ.Kidney, RepeatType.Single, Organism.Rat)));
		hp.add(datasetButton("Rat, in vivo, kidney, repeat", new DataFilter(
				CellType.Vivo, Organ.Kidney, RepeatType.Repeat, Organism.Rat)));

		return hp;
	}
	
	private Widget datasetButton(final String title, final DataFilter filter) {
		Button b = new Button(title);
		b.setStyleName("slightlySpaced");
		b.setWidth("100%");
		b.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ce) {
				changeDataFilter(filter);
				String vt = valueTypeList.getItemText(valueTypeList.getSelectedIndex());
				changeValueType(ValueType.valueOf(vt));
				storeState();
				History.newItem("compound"); //Go to compound selection screen
			}
		});
		return b;
	}
}
