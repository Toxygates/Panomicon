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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
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
	
	final DatasetScreen ds = this;
	
	private abstract class GUI {
		abstract DataFilter[] filters();
		Widget content() {
			Grid g = new Grid(3, 2);
			HorizontalPanel hp = Utils.mkWidePanel();
			hp.setHeight("100%");
			VerticalPanel vp = Utils.mkTallPanel();
			HTML banner = new HTML(resources.bannerHTML().getText());
			banner.setWidth("40em");
			vp.add(banner);			
			vp.add(g);
			HTML latest = new HTML(resources.latestVersionHTML().getText());
			latest.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
			vp.add(latest);
			latest.setWidth("40em");
			hp.add(vp);
			g.setCellSpacing(20);
			fillGrid(g, 3, filters());	
			return hp;
		}
		
		void fillGrid(Grid g, int columns, DataFilter[] filters) {
			int r = 0;
			int c = 0;
			for (DataFilter f: filters) {
				g.setWidget(r, c, new DatasetInfo(f, ds));
				c += 1;
				if (c == columns - 1) {
					r += 1;
					c = 0;
				}
			}		
		}		
	}

	private class Toxygates extends GUI {		
		DataFilter[] filters() {
			return new DataFilter[] {		
					new DataFilter(CellType.Vitro, Organ.Kidney, RepeatType.Single, Organism.Human),
					new DataFilter(CellType.Vitro, Organ.Kidney, RepeatType.Single, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Single, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Repeat, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Kidney, RepeatType.Single, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Kidney, RepeatType.Repeat, Organism.Rat)
			};		
		}
	}
	
	private class Adjuvant extends GUI {
		DataFilter[] filters() { 
			return new DataFilter[] {			
					new DataFilter(CellType.Vivo, Organ.Kidney, RepeatType.Single, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Single, Organism.Rat),				
					new DataFilter(CellType.Vivo, Organ.Lung, RepeatType.Single, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Muscle, RepeatType.Single, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Spleen, RepeatType.Single, Organism.Rat)
			};
		}
	}
	
	public Widget content() {
		final String uitype = manager.getUIType();
		GUI gui;
		if ("toxygates".equals(uitype)) {
			gui = new Toxygates();
		} else if ("adjuvant".equals(uitype)){
			gui = new Adjuvant();
		} else {
			// Graceful default
			gui = new Toxygates();
		}
		return gui.content();
	}
	
	public void filterSelected(DataFilter filter) {		
		Storage s = tryGetStorage();
		if (s != null) {		
			changeDataFilter(filter);
			storeDataFilter(s);
			setConfigured(true);
			manager.deconfigureAll(this);
			configuredProceed(ColumnScreen.key);
		} else {
			Window.alert("Your browser does not support local storage. Unable to continue.");
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
