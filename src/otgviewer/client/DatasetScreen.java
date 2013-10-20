package otgviewer.client;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
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
		return adjuvantContent();
//		return toxygatesContent();
	}
	
	/**
	 * For the main Toxygates instance
	 * TODO: find a better way of configuring this
	 * @return
	 */
	private Widget toxygatesContent() {
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
		fillGrid(g, 3, filters);	
		return hp;
	}
	
	/**
	 * For the Adjuvant instance
	 * TODO as above
	 * @return
	 */
	private Widget adjuvantContent() {
		Grid g = new Grid(3, 2);
		HorizontalPanel hp = Utils.mkWidePanel();
		hp.setHeight("100%");		
		final DataFilter[] filters = new DataFilter[] {				
				new DataFilter(CellType.Vivo, Organ.Kidney, RepeatType.Single, Organism.Rat),
				new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Single, Organism.Rat),				
				new DataFilter(CellType.Vivo, Organ.Lung, RepeatType.Single, Organism.Rat),
				new DataFilter(CellType.Vivo, Organ.Muscle, RepeatType.Single, Organism.Rat),
				new DataFilter(CellType.Vivo, Organ.Spleen, RepeatType.Single, Organism.Rat)
		};
		hp.add(g);
		g.setCellSpacing(20);
		fillGrid(g, 3, filters);	
		return hp;		
	}
	
	private void fillGrid(Grid g, int columns, DataFilter[] filters) {
		int r = 0;
		int c = 0;
		for (DataFilter f: filters) {
			g.setWidget(r, c, new DatasetInfo(f, this));
			c += 1;
			if (c == columns - 1) {
				r += 1;
				c = 0;
			}
		}		
	}
	
	public void filterSelected(DataFilter filter) {		
		changeDataFilter(filter);
		Storage s = tryGetStorage();
		if (s != null) {			
			storeDataFilter(s);
			setConfigured(true);
			manager.deconfigureAll(this);
			configuredProceed(ColumnScreen.key);
		}
	}

	@Override
	public void tryConfigure() {
		if (chosenDataFilter != null) {
			setConfigured(true);
		}
	}

	@Override
	public String getGuideText() {
		return "Welcome to Toxygates. Click on one of the six datasets below to proceed.";
	}
}
