package otgviewer.client;

import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.shared.DataFilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A widget that displays times and doses for a number of compounds in a grid
 * layout. Each time and dose combination can be selected individually.
 * @author johan
 *
 */
abstract public class TimeDoseGrid extends DataListenerWidget {
	private Grid grid = new Grid();
	protected String[] availableTimes = null;
	protected VerticalPanel rootPanel;
	protected VerticalPanel mainPanel;
	
	protected SparqlServiceAsync owlimService = (SparqlServiceAsync) GWT
			.create(SparqlService.class);

	protected void initTools(HorizontalPanel toolPanel) {
		
	}
	
	public TimeDoseGrid() {
		rootPanel = Utils.mkVerticalPanel();
		initWidget(rootPanel);
		rootPanel.setWidth("730px");
		mainPanel = new VerticalPanel();
		
		
		HorizontalPanel selectionPanel = Utils.mkHorizontalPanel();		
		mainPanel.add(selectionPanel);
		initTools(selectionPanel);
		selectionPanel.setSpacing(2);
		
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
			fetchTimes();
		} else {
			super.dataFilterChanged(filter);
		}		
	}
	
	
	@Override
	public void compoundsChanged(List<String> compounds) {				
		super.compoundsChanged(compounds);		
		if (compounds.isEmpty()) {
			rootPanel.clear();
			rootPanel.add(Utils.mkEmphLabel("Please select at least one compound"));
		} else {
			rootPanel.clear();
			rootPanel.add(mainPanel);
			redrawGrid();
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
		owlimService.times(chosenDataFilter, null, new AsyncCallback<String[]>() {
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

	protected int doseToIndex(String dose) {
		if (dose.equals("Low")) {
			return 0;
		} else if (dose.equals("Middle")) {
			return 1;
		} else {
			return 2;
		}
	}
	
	protected String indexToDose(int dose) {
		switch (dose) {
		case 0:
			return "Low";			
		case 1:
			return "Middle";								
		}
		return "High";
	}
	
	private void redrawGrid() {
		grid.resize(chosenCompounds.size() + 1, 4);
		
		for (int i = 1; i < chosenCompounds.size() + 1; ++i) {
			
			grid.setWidget(i, 0, Utils.mkEmphLabel(chosenCompounds.get(i - 1)));
		}
				
		grid.setWidget(0, 1, Utils.mkEmphLabel("Low"));		
		grid.setWidget(0, 2, Utils.mkEmphLabel("Medium"));		
		grid.setWidget(0, 3, Utils.mkEmphLabel("High"));
		
		grid.setHeight(50 * (chosenCompounds.size() + 1) + "px");
		lazyFetchTimes();		
	}
	
	abstract protected Widget initUnit(int compound, int dose, int time);
	protected Widget finaliseGroup(int compound, int dose) {
		return null;
	}

	protected void drawGridInner(Grid grid) {
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			for (int d = 0; d < 3; ++d) {
				HorizontalPanel hp = Utils.mkHorizontalPanel(true);
				for (int t = 0; t < availableTimes.length; ++t) {
					hp.add(initUnit(c, d, t));
				}
				Widget fin = finaliseGroup(c, d);
				if (fin != null) {
					hp.add(fin);
				}

				SimplePanel sp = new SimplePanel(hp);
				sp.setStyleName("border");
				grid.setWidget(c + 1, d + 1, sp);
			}
		}
	}
	
}
