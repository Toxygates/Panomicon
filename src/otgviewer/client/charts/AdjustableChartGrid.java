package otgviewer.client.charts;

import java.util.Arrays;
import java.util.List;

import otgviewer.client.OwlimService;
import otgviewer.client.OwlimServiceAsync;
import otgviewer.client.Utils;
import otgviewer.client.components.Screen;
import otgviewer.shared.Group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class AdjustableChartGrid extends Composite {
	private ListBox chartCombo, chartSubtypeCombo;
	
	private ChartDataSource source;
	private List<String> compounds;
	private List<Group> groups;
	private VerticalPanel vp;
	private VerticalPanel ivp;
	private Screen screen;
	
	public AdjustableChartGrid(Screen screen, ChartDataSource source, List<Group> groups) {
		this.source = source;
		this.groups = groups;
		this.screen = screen;
		this.compounds = Arrays.asList(Utils.compoundsFor(groups));		
		
		vp = Utils.mkVerticalPanel();
		initWidget(vp);
//		vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		
		HorizontalPanel hp = Utils.mkHorizontalPanel();		
		vp.add(hp);
		
		hp.setStyleName("colored");
		hp.setWidth("100%");
		
		HorizontalPanel ihp = Utils.mkHorizontalPanel();
		hp.add(ihp);
		ihp.setSpacing(5);
		
		chartCombo = new ListBox();
		ihp.add(chartCombo);
		
		chartCombo.addItem("Expression vs time, fixed dose:");
		chartCombo.addItem("Expression vs dose, fixed time:");
		chartCombo.setSelectedIndex(0);

		chartSubtypeCombo = new ListBox();
		ihp.add(chartSubtypeCombo);

		chartSubtypeCombo.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {				
				redraw(false);
			}

		});
		chartCombo.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				updateSeriesSubtypes();
			}
		});
		
		ivp = Utils.mkVerticalPanel();
		vp.add(ivp);
		redraw(false);
	}
	
	//vsTime is the vs-time-ness of each individual sub-chart. So the overall grid will be vs. dose 	
	//(in its columns) if each sub-chart is vs.time.
	private ChartGrid gridFor(boolean vsTime, String[] columns, String[] useCompounds) {
		String[] useColumns = (columns == null ? (vsTime ? source.doses() : source.times()) : columns);
		ChartTables ct = (groups != null) ? 
			new ChartTables.GroupedChartTable(source.getSamples(useCompounds), groups, 
					vsTime ? source.times() : source.doses(), vsTime)
		:
			new ChartTables.PlainChartTable(source.getSamples(useCompounds), vsTime ? source.times() : source.doses(), vsTime);
					
		
		return new ChartGrid(screen, ct, groups, useCompounds == null ? compounds : Arrays.asList(useCompounds), true, 
				useColumns, !vsTime);
	}
	
	public void redraw(boolean fromUpdate) {

		// make sure something is selected
		if (chartCombo.getSelectedIndex() == -1) {
			chartCombo.setSelectedIndex(0);
		}
		if (chartSubtypeCombo.getItemCount() == 0 && !fromUpdate) {
			updateSeriesSubtypes(); // will redraw for us later
		} else {

			if (chartSubtypeCombo.getSelectedIndex() == -1) {
				chartSubtypeCombo.setSelectedIndex(0);
			}
			
			ivp.clear();
								
			String subtype = chartSubtypeCombo.getItemText(chartSubtypeCombo
									.getSelectedIndex());
			String[] columns = (subtype.equals("All") ? null : new String[] { subtype } );
			
			if (groups != null) {
				for (Group g : groups) {
					Label l = new Label("Compounds in '" + g.getName() + "'");
					l.setStyleName("heading");
					ivp.add(l);
					ivp.add(gridFor(chartCombo.getSelectedIndex() == 0,
							columns, g.getCompounds()));
				}
			} else {
				ivp.add(gridFor(chartCombo.getSelectedIndex() == 0, columns, null));
			}
			
		}
	}
	
	private void updateSeriesSubtypes() {
		chartSubtypeCombo.clear();
		if (chartCombo.getSelectedIndex() == 0) {
			for (String dose: source.doses()) {
				chartSubtypeCombo.addItem(dose);
			}
		} else {			
			for (String time: source.times()) {
				chartSubtypeCombo.addItem(time);
			}
		}
		
		if (chartSubtypeCombo.getItemCount() > 0) {
			chartSubtypeCombo.addItem("All");
			chartSubtypeCombo.setSelectedIndex(chartSubtypeCombo.getItemCount() - 1);
			redraw(true);
		}
	}
	
}
