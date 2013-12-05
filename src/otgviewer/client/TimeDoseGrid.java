package otgviewer.client;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.Screen;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataFilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A widget that displays times and doses for a number of compounds in a grid
 * layout. For each position in the grid, an arbitrary widget can be displayed.
 * @author johan
 *
 */
abstract public class TimeDoseGrid extends DataListenerWidget {
	private Grid grid = new Grid();
	protected String[] availableTimes = null;
	
	protected VerticalPanel rootPanel;
	protected VerticalPanel mainPanel;
	
	protected final boolean hasDoseTimeGUIs;
	
	protected SparqlServiceAsync sparqlService = (SparqlServiceAsync) GWT
			.create(SparqlService.class);

	private Screen screen;
	
	protected Map<String, List<Barcode>> availableSamples = new HashMap<String, List<Barcode>>();
	
	/** 
	 * To be overridden by subclasses
	 * @param toolPanel
	 */
	protected void initTools(HorizontalPanel toolPanel) { }
	
	public TimeDoseGrid(Screen screen, boolean hasDoseTimeGUIs) {
		rootPanel = Utils.mkVerticalPanel();
		this.screen = screen;
		initWidget(rootPanel);
		rootPanel.setWidth("730px");
		mainPanel = new VerticalPanel();
		
		HorizontalPanel selectionPanel = Utils.mkHorizontalPanel();		
		mainPanel.add(selectionPanel);
		initTools(selectionPanel);
		selectionPanel.setSpacing(2);
		
		this.hasDoseTimeGUIs = hasDoseTimeGUIs;
		
		grid.setStyleName("highlySpaced");
		grid.setWidth("100%");
		grid.setHeight("400px");
		grid.setBorderWidth(0);
		mainPanel.add(grid);
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {
		if (!filter.equals(chosenDataFilter)) {
			super.dataFilterChanged(filter);
			availableTimes = null;
			screen.enqueue(new Screen.QueuedAction("fetchTimes") {				
				@Override
				public void run() {
					fetchTimes();					
				}
			});
		} else {
			super.dataFilterChanged(filter);
		}		
	}
	
	@Override
	public void compoundsChanged(List<String> compounds) {				
		super.compoundsChanged(compounds);		
		rootPanel.clear();		
		if (compounds.isEmpty()) {
			rootPanel.add(Utils.mkEmphLabel("Please select at least one compound"));
		} else {
			rootPanel.add(mainPanel);
			redrawGrid();
			fetchSamples();
		}
	}
	
	private void lazyFetchTimes() {
		if (availableTimes != null && availableTimes.length > 0) {
			drawGridInner(grid);
		} else {
			fetchTimes();						
		}		
	}
	
	private void fetchTimes() {		
		sparqlService.times(chosenDataFilter, null, new AsyncCallback<String[]>() {
			public void onSuccess(String[] times) {
				availableTimes = times;
				drawGridInner(grid);
				//TODO: block compound selection until we have obtained this data
			}
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get sample times.");
			}
		});			
	}
	
	protected String keyFor(Barcode b) {
		return b.getCompound() + ":" + b.getDose() + ":" + b.getTime();
	}
	
	protected void fetchSamples() {
		availableSamples.clear();
		String[] compounds = chosenCompounds.toArray(new String[0]);
		sparqlService.barcodes(chosenDataFilter, compounds,
				null, null, new AsyncCallback<Barcode[]>() {

			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Unable to obtain samples.");				
			}

			@Override
			public void onSuccess(Barcode[] result) {
				for (Barcode b: result) {
					String k = keyFor(b);
					if (!availableSamples.containsKey(k)) {						
						availableSamples.put(k, new LinkedList<Barcode>());
					}					
					availableSamples.get(k).add(b);
					Window.alert(availableSamples.get(k).size() + "");
				}	
				samplesAvailable();
			}			
		});
	}
	
	protected void samplesAvailable() { }

	protected int doseToIndex(String dose) {
		if (dose.equals("Low")) {
			return 0;
		} else if (dose.equals("Middle")) {
			return 1;
		} else if (dose.equals("High")){
			return 2;
		} else {
			return -1;
		}
	}
	
	protected String indexToDose(int dose) {
		switch (dose) {
		case 0:
			return "Low";			
		case 1:
			return "Middle";
		case 2: 
			return "High";
		}
		return null;
	}
	
	protected int numDoses() {
		return 3;
	}
	
	private void redrawGrid() {
		final int numRows = chosenCompounds.size() + 1 + (hasDoseTimeGUIs ? 1 : 0);
		// TODO don't use magic numbers like 4
		grid.resize(numRows, 4);
		
		int r = 0;
		for (int i = 0; i < numDoses(); ++i) {
			grid.setWidget(r, i + 1, Utils.mkEmphLabel(indexToDose(i)));
		}
		r++;
				
		if (hasDoseTimeGUIs) {
			grid.setWidget(r, 0, new Label("All"));
			r++;
		}

		for (int i = 1; i < chosenCompounds.size() + 1; ++i) {			
			grid.setWidget(r, 0, Utils.mkEmphLabel(chosenCompounds.get(i - 1)));
			r++;
		}
		
		grid.setHeight(50 * (chosenCompounds.size() + 1) + "px");
		lazyFetchTimes();		
	}
	
	/**
	 * Obtain the widget to display for a compound/dose/time combination.
	 * @param compound
	 * @param dose
	 * @param time
	 * @return
	 */
	abstract protected Widget guiFor(int compound, int dose, int time);
	
	/**
	 * An optional extra widget on the right hand side of a compound/dose combination.
	 * @param compound
	 * @param dose
	 * @return
	 */
	protected Widget guiForCompoundDose(int compound, int dose) {
		return null;
	}
	
	/**
	 * An optional extra widget above all compounds for a given time/dose combination.
	 * @param compound
	 * @param time
	 * @return
	 */
	protected Widget guiForDoseTime(int dose, int time) {
		return null;
	}

	protected void drawGridInner(Grid grid) {
		int r = 1;
		if (hasDoseTimeGUIs && chosenCompounds.size() > 0) {
			for (int d = 0; d < numDoses(); ++d) {
				HorizontalPanel hp = Utils.mkHorizontalPanel(true);
				for (int t = 0; t < availableTimes.length; ++t) {
					hp.add(guiForDoseTime(d, t));
				}
				SimplePanel sp = new SimplePanel(hp);
				sp.setStyleName("invisibleBorder");				
				grid.setWidget(r, d + 1, hp);
			}
			r++;
		}
		
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			for (int d = 0; d < numDoses(); ++d) {
				HorizontalPanel hp = Utils.mkHorizontalPanel(true);
				for (int t = 0; t < availableTimes.length; ++t) {
					hp.add(guiFor(c, d, t));
				}
				Widget fin = guiForCompoundDose(c, d);
				if (fin != null) {
					hp.add(fin);
				}

				SimplePanel sp = new SimplePanel(hp);
				sp.setStyleName("border");
				grid.setWidget(r, d + 1, sp);
			}
			r++;
		}
	}
	
}
