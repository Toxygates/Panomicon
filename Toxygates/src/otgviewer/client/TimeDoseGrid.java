/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import t.common.shared.DataSchema;
import t.common.shared.Pair;
import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.viewer.client.Utils;
import t.viewer.client.rpc.SparqlServiceAsync;

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
	
	protected VerticalPanel rootPanel;
	protected VerticalPanel mainPanel;
	
	protected final boolean hasDoseTimeGUIs;
	
	protected final SparqlServiceAsync sparqlService;

	protected Screen screen;
	protected final String majorParameter, 
		mediumParameter, minorParameter, timeParameter;
	protected final DataSchema schema;
	
	protected List<String> mediumValues = new ArrayList<String>();
	protected List<String> minorValues = new ArrayList<String>();
	
	//First pair member: treated samples, second: control samples or null
	protected Pair<Unit, Unit>[] availableUnits;
	protected Logger logger = SharedUtils.getLogger("tdgrid");
	
	protected String emptyMessage;
	
	/** 
	 * To be overridden by subclasses
	 * @param toolPanel
	 */
	protected void initTools(HorizontalPanel toolPanel) { }
	
	public TimeDoseGrid(Screen screen, boolean hasDoseTimeGUIs) {
		rootPanel = Utils.mkVerticalPanel();
		this.screen = screen;
		this.sparqlService = screen.sparqlService();
		initWidget(rootPanel);
		rootPanel.setWidth("730px");
		mainPanel = new VerticalPanel();
		this.schema = screen.schema();
		this.majorParameter = schema.majorParameter();
		this.mediumParameter = schema.mediumParameter();
		this.minorParameter = schema.minorParameter();
		this.timeParameter = schema.timeParameter();
		try {			
			mediumValues = new ArrayList<String>();
			String[] mvs = schema.sortedValues(schema.mediumParameter());
			for (String v: mvs) {
				if (!schema.isControlValue(v)) {
					mediumValues.add(v);
				}
			}
		} catch (Exception e) {
			logger.warning("Unable to sort medium parameters");
		}
		
		logger.info("Medium: " + mediumParameter + " minor: " + minorParameter);
		
		HorizontalPanel selectionPanel = Utils.mkHorizontalPanel();		
		mainPanel.add(selectionPanel);
		initTools(selectionPanel);
		selectionPanel.setSpacing(2);
		
		this.hasDoseTimeGUIs = hasDoseTimeGUIs;
		String mtitle = schema.title(majorParameter);
		emptyMessage = "Please select at least one " + mtitle;
		
		grid.setStylePrimaryName("highlySpaced");
		grid.setWidth("100%");
		grid.setHeight("400px");
		grid.setBorderWidth(0);
		mainPanel.add(grid);
	}
	
	@Override
	public void sampleClassChanged(SampleClass sc) {
		if (!sc.equals(chosenSampleClass)) {
			super.sampleClassChanged(sc);
			minorValues = new ArrayList<String>();			
			screen.enqueue(new Screen.QueuedAction("fetchTimes") {				
				@Override
				public void run() {
					fetchMinor();					
				}
			});
		} else {
			super.sampleClassChanged(sc);
		}		
	}
	
	@Override
	public void compoundsChanged(List<String> compounds) {				
		super.compoundsChanged(compounds);		
		rootPanel.clear();		
		if (compounds.isEmpty()) {
			setEmptyMessage(emptyMessage);			
		} else {
			rootPanel.add(mainPanel);
			redrawGrid();
			fetchSamples();
		}
	}
	
	/**
	 * Change the message that is to be displayed when no major 
	 * values have been selected.
	 * @param message
	 */
	public void setEmptyMessage(String message) {
		this.emptyMessage = message;
		if (chosenCompounds.isEmpty()) {
			rootPanel.add(Utils.mkEmphLabel(emptyMessage));
		}
	}
	
	private void lazyFetchMinor() {
		if (minorValues != null && minorValues.size() > 0) {
			logger.info("Reuse cached minor values");
			drawGridInner(grid);
		} else {
			fetchMinor();						
		}		
	}
	
	private void fetchMinor() {		
		logger.info("Fetch minor");
		sparqlService.parameterValues(chosenSampleClass, minorParameter,
				new PendingAsyncCallback<String[]>(this, "Unable to fetch minor parameter for samples") {
			public void handleSuccess(String[] times) {
				try {
					logger.info("Sort " + times.length + " times");
					schema.sort(minorParameter, times);
					minorValues = Arrays.asList(times);				
					drawGridInner(grid);
					//TODO: block compound selection until we have obtained this data
				} catch (Exception e) {
					logger.warning("Unable to sort times " + e.getMessage());
				}
			}			
		});			
	}
	
	protected String keyFor(Sample b) {
		//TODO efficiency
		return b.sampleClass().tripleString(schema);		
	}
	
	private boolean fetchingSamples = false;
	private String[] fetchingForCompounds = new String[0];
	
	protected void fetchSamples() {
		if (fetchingSamples) {
			return;
		}
		fetchingSamples = true;
		availableUnits = new Pair[0]; 
		String[] compounds = chosenCompounds.toArray(new String[0]);
		fetchingForCompounds = compounds;
		sparqlService.units(chosenSampleClass, majorParameter, compounds,
				new PendingAsyncCallback<Pair<Unit, Unit>[]>(this, "Unable to obtain samples.") {

			@Override
			public void handleFailure(Throwable caught) {
				super.handleFailure(caught);		
				fetchingSamples = false;
			}

			@Override
			public void handleSuccess(Pair<Unit, Unit>[] result) {
				availableUnits = result;							
				samplesAvailable();				
				fetchingSamples = false;
				if (!Arrays.equals(fetchingForCompounds, 
				    chosenCompounds.toArray(new String[0]))) {
				    //Fetch again - the compounds changed while we were loading data
				    fetchSamples();
				}
			}			
		});
	}
	
	protected void samplesAvailable() { }

	private void redrawGrid() {
		final int numRows = chosenCompounds.size() + 1 + (hasDoseTimeGUIs ? 1 : 0);

		grid.resize(numRows, mediumValues.size() + 1);
		
		int r = 0;
		for (int i = 0; i < mediumValues.size(); ++i) {
			grid.setWidget(r, i + 1, Utils.mkEmphLabel(mediumValues.get(i)));
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
		//This will eventually draw the unit UIs
		lazyFetchMinor();		
	}
	
	/**
	 * Obtain the widget to display for a compound/dose/time combination.
	 * @param compound
	 * @param dose
	 * @param time
	 * @return
	 */
	abstract protected Widget guiForUnit(Unit unit);
	
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
		final int numMed = mediumValues.size();
		final int numMin = minorValues.size();
		logger.info("Draw grid inner: " + numMed + ", " + numMin);
		if (hasDoseTimeGUIs && chosenCompounds.size() > 0) {
			for (int d = 0; d < numMed; ++d) {
				HorizontalPanel hp = Utils.mkHorizontalPanel(true);
				for (int t = 0; t < numMin; ++t) {
					hp.add(guiForDoseTime(d, t));
				}
				SimplePanel sp = new SimplePanel(hp);
				sp.setStylePrimaryName("invisibleBorder");				
				grid.setWidget(r, d + 1, hp);
			}
			r++;
		}
		
		List<Pair<Unit, Unit>> allUnits = new ArrayList<Pair<Unit, Unit>>();
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			for (int d = 0; d < numMed; ++d) {
				HorizontalPanel hp = Utils.mkHorizontalPanel(true);				
				for (int t = 0; t < numMin; ++t) {
					SampleClass sc = new SampleClass();
					sc.put(majorParameter, chosenCompounds.get(c));
					sc.put(mediumParameter, mediumValues.get(d));
					sc.put(minorParameter, minorValues.get(t));
					sc.mergeDeferred(chosenSampleClass);
					Unit unit = new Unit(sc, new Sample[] {});									
					allUnits.add(new Pair<Unit, Unit>(unit, null)); 
					hp.add(guiForUnit(unit));
				}
				Widget fin = guiForCompoundDose(c, d);
				if (fin != null) {
					hp.add(fin);
				}

				SimplePanel sp = new SimplePanel(hp);
				sp.setStylePrimaryName("border");
				grid.setWidget(r, d + 1, sp);
			}
			r++;
		}
		availableUnits = allUnits.toArray(new Pair[0]);
	}
	
}
