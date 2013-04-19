package otgviewer.client;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This is the first screen, where a dataset can be selected.
 */
public class DatasetScreen extends Screen implements DatasetInfo.SelectionListener {
	static String key = "ds";	
	VerticalPanel vp;
	
	public DatasetScreen(ScreenManager man) {
		super("Dataset selection", key, false, false, man);		
	}
	
	public Widget content() {
		Grid g = new Grid(3, 2);

		HorizontalPanel hp = Utils.mkWidePanel();
		hp.setHeight("100%");

		final DataFilter[] filters = new DataFilter[] {
				new DataFilter(CellType.Vitro, Organ.Kidney, RepeatType.Single, Organism.Human),
				new DataFilter(CellType.Vitro, Organ.Kidney, RepeatType.Single, Organism.Rat),
				new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Single, Organism.Rat),
				new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Repeat, Organism.Rat),
				new DataFilter(CellType.Vivo, Organ.Kidney, RepeatType.Single, Organism.Rat),
				new DataFilter(CellType.Vivo, Organ.Kidney, RepeatType.Repeat, Organism.Rat)
		};
		hp.add(g);
		g.setCellSpacing(20);
		
		int r = 0;
		int c = 0;
		for (DataFilter f: filters) {
			g.setWidget(r, c, new DatasetInfo(f, this));
			c += 1;
			if (c == 2) {
				r += 1;
				c = 0;
			}
		}	
		return hp;
	}
	
	public void filterSelected(DataFilter filter) {		
		changeDataFilter(filter);
		storeDataFilter();
		setConfigured(true);
		manager.deconfigureAll(this);
		configuredProceed(ColumnScreen.key);		
	}

	@Override
	public void tryConfigure() {
		if (chosenDataFilter != null) {
			setConfigured(true);
		}
	}

}
