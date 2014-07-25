package otgviewer.client;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.StorageParser;
import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;

import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This is the first screen, where a dataset can be selected.
 */
public class DatasetScreen extends Screen implements DatasetInfo.SelectionListener {
	public static String key = "ds";	
	VerticalPanel vp;
	
	public DatasetScreen(ScreenManager man) {
		super("Dataset selection", key, false, man);		
	}
	
	final DatasetScreen ds = this;
	
	private abstract class GUI {		
		abstract TextResource topBanner();

		abstract DatasetInfo[][] makeDatasetInfo();
		
		final private HTML newsHtml = new HTML();
		
		Widget content() {
			DatasetInfo[][] infos = makeDatasetInfo();
			Grid g = new Grid(infos.length, infos[0].length);
			HorizontalPanel hp = Utils.mkWidePanel();
			hp.setHeight("100%");
			VerticalPanel vp = Utils.mkTallPanel();
			HTML banner = new HTML(topBanner().getText());
			banner.setWidth("40em");
			vp.add(banner);			
			vp.add(g);

			vp.add(newsHtml);
			newsHtml.setWidth("40em");
			Utils.loadHTML("news.html", new Utils.HTMLCallback() {				
				@Override
				protected void setHTML(String html) {
					newsHtml.setHTML(html);
				}			
			});
			
			hp.add(vp);
			g.setCellSpacing(10);
			fillGrid(g, infos);	
			return Utils.makeScrolled(hp);
		}
		
		void fillGrid(Grid g, DatasetInfo[][] infos) {
			final int rows = infos.length;
			final int columns = infos[0].length;
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					if (infos[r][c] != null) {
						g.setWidget(r, c, infos[r][c]);
					}
				}
			}			
		}				
	}

	private class Toxygates extends GUI {
		DatasetInfo[][] makeDatasetInfo() {
			DataFilter[] filters = new DataFilter[] {		
					new DataFilter(CellType.Vitro, Organ.Kidney, RepeatType.Single, Organism.Human),
					new DataFilter(CellType.Vitro, Organ.Kidney, RepeatType.Single, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Single, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Repeat, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Kidney, RepeatType.Single, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Kidney, RepeatType.Repeat, Organism.Rat)
			};		
			
			return new DatasetInfo[][] { 
					{ makeInfo(filters[0]), makeInfo(filters[1]) },
					{ makeInfo(filters[2]), makeInfo(filters[3]) },
					{ makeInfo(filters[4]), makeInfo(filters[5]) }					
			};
		}
		
		TextResource topBanner() {
			return resources.bannerHTML();
		}
		
		DatasetInfo makeInfo(DataFilter filter) {
			return new DatasetInfo(filter, ds, true);
		}
	}
	
	private class Adjuvant extends GUI {
		DatasetInfo[][] makeDatasetInfo() {
			DataFilter[] filters = new DataFilter[] {			
					new DataFilter(CellType.Vivo, Organ.Kidney, RepeatType.Single, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Single, Organism.Rat),				
					new DataFilter(CellType.Vivo, Organ.Lung, RepeatType.Single, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Muscle, RepeatType.Single, Organism.Rat),
					new DataFilter(CellType.Vivo, Organ.Spleen, RepeatType.Single, Organism.Rat),
					
					new DataFilter(CellType.Vivo, Organ.Kidney, RepeatType.Single, Organism.Mouse),
					new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Single, Organism.Mouse),
					new DataFilter(CellType.Vivo, Organ.Spleen, RepeatType.Single, Organism.Mouse),
					new DataFilter(CellType.Vivo, Organ.Lung, RepeatType.Single, Organism.Mouse),
					new DataFilter(CellType.Vivo, Organ.LymphNode, RepeatType.Single, Organism.Mouse)			
			};
			return new DatasetInfo[][] { 
					{ makeInfo(filters[0]), makeInfo(filters[1]), makeInfo(filters[2]) },
					{ makeInfo(filters[3]), makeInfo(filters[4]), null },
					{ makeInfo(filters[5]), makeInfo(filters[6]), makeInfo(filters[7]) },
					{ makeInfo(filters[8]), makeInfo(filters[9]), null },
			};
		}

		TextResource topBanner() {
			return resources.adjuvantBannerHTML();
		}
		
		DatasetInfo makeInfo(DataFilter filter) {
			return new DatasetInfo(filter, ds, false);
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
		StorageParser p = getParser(this);
		changeDataFilter(filter);
		storeDataFilter(p);
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

	@Override
	public String getGuideText() {
		return "Welcome to Toxygates. Click on one of the six datasets below to proceed.";
	}
}
