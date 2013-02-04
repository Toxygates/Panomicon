package otgviewer.client.charts;

import java.util.Arrays;
import java.util.List;

import otgviewer.client.OwlimService;
import otgviewer.client.OwlimServiceAsync;
import otgviewer.client.Utils;
import otgviewer.shared.Group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class AdjustableChartGrid extends Composite {
	private ListBox chartCombo, chartSubtypeCombo;
	private ChartGrid cg;
	private ChartDataSource source;
	private List<String> compounds;
	private List<Group> groups;
	private VerticalPanel vp;
	
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);
	
	public AdjustableChartGrid(ChartDataSource source, List<Group> groups) {
		this.source = source;
		this.groups = groups;
		this.compounds = Arrays.asList(Utils.compoundsFor(groups));		
		
		vp = new VerticalPanel();
		initWidget(vp);
		
		HorizontalPanel hp = Utils.mkHorizontalPanel();
		vp.add(hp);
		
		chartCombo = new ListBox();
		hp.add(chartCombo);
		chartCombo.addItem("Expression vs time, fixed dose:");
		chartCombo.addItem("Expression vs dose, fixed time:");
		chartCombo.setSelectedIndex(0);

		chartSubtypeCombo = new ListBox();
		hp.add(chartSubtypeCombo);

		chartSubtypeCombo.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {				
				redraw();
			}

		});
		chartCombo.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				updateSeriesSubtypes();
			}
		});
		
		cg = gridFor(true, null );
		
		vp.add(cg);		
	}
	
	//vsTime is the vs-time-ness of each individual sub-chart. So the overall grid will be vs. dose 	
	//(in its columns) if each sub-chart is vs.time.
	private ChartGrid gridFor(boolean vsTime, String[] columns) {
		String[] useColumns = (columns == null ? (vsTime ? source.doses() : source.times()) : columns);
		ChartTables ct = (groups != null) ? 
			new ChartTables.GroupedChartTable(source.getSamples(), groups, 
					vsTime ? source.times() : source.doses(), vsTime)
		:
			new ChartTables.PlainChartTable(source.getSamples(), vsTime ? source.times() : source.doses(), vsTime);
		return new ChartGrid(ct, groups, compounds, true, 
				useColumns, !vsTime);
	}
	
	public void redraw() {

		// make sure something is selected
		if (chartCombo.getSelectedIndex() == -1) {
			chartCombo.setSelectedIndex(0);
		}
		if (chartSubtypeCombo.getItemCount() == 0) {
			updateSeriesSubtypes(); // will redraw for us later
		} else {

			if (chartSubtypeCombo.getSelectedIndex() == -1) {
				chartSubtypeCombo.setSelectedIndex(0);
			}
			
			if (cg != null) {
				vp.remove(cg);
			}
					
			String subtype = chartSubtypeCombo.getItemText(chartSubtypeCombo
									.getSelectedIndex());
			String[] columns = (subtype.equals("All") ? null : new String[] { subtype } );
			
			cg = gridFor(chartCombo.getSelectedIndex() == 0, columns);
			vp.add(cg);
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
			chartSubtypeCombo.setSelectedIndex(0);
			redraw();
		}
	}
	
}
