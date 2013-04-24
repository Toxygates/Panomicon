package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import otgviewer.client.Utils;
import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.client.components.Screen;
import otgviewer.shared.Group;
import otgviewer.shared.OTGUtils;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class AdjustableChartGrid extends Composite {
	private ListBox chartCombo, chartSubtypeCombo;
	
	private ChartDataSource source;
	private List<String> compounds;
	private List<Group> groups;
	private VerticalPanel vp;
	private VerticalPanel ivp;
	private Screen screen;
	
	private static int lastType = -1, lastSubtype = -1;
	
	public AdjustableChartGrid(Screen screen, ChartDataSource source, List<Group> groups) {
		this.source = source;
		this.groups = groups;
		this.screen = screen;
		this.compounds = Arrays.asList(OTGUtils.compoundsFor(groups));		
		
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
		setType( (lastType == -1 ? 0 : lastType));

		chartSubtypeCombo = new ListBox();
		ihp.add(chartSubtypeCombo);

		chartSubtypeCombo.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {								
				lastSubtype = chartSubtypeCombo.getSelectedIndex();
				redraw(false);
			}

		});
		chartCombo.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				lastType = chartCombo.getSelectedIndex();
				lastSubtype = -1;
				updateSeriesSubtypes();
			}
		});
		
		ivp = Utils.mkVerticalPanel();
		vp.add(ivp);
		redraw(false);
	}
	
	//vsTime is the vs-time-ness of each individual sub-chart. So the overall grid will be vs. dose 	
	//(in its columns) if each sub-chart is vs.time.
	private void gridFor(final boolean vsTime, final String[] columns, final String[] useCompounds, 
			final List<ChartGrid> intoList, final SimplePanel intoPanel) {
		final String[] useColumns = (columns == null ? (vsTime ? source.doses() : source.times()) : columns);
		source.getSamples(useCompounds, useColumns, 
				new ChartDataSource.SampleAcceptor() {
					@Override
					public void accept(List<ChartSample> samples) {
						ChartTables ct = (groups != null) ? 
								new ChartTables.GroupedChartTable(samples, samples, groups, 
										vsTime ? source.times() : source.doses(), vsTime)
							:
								new ChartTables.PlainChartTable(samples, samples, 
										vsTime ? source.times() : source.doses(), vsTime);
										
							
							ChartGrid cg = new ChartGrid(screen, ct, groups, useCompounds == null ? compounds : Arrays.asList(useCompounds), true, 
									useColumns, -1, !vsTime, 780);
							
							intoList.add(cg);
							intoPanel.add(cg);
							intoPanel.setHeight("");
							
							expectedGrids -= 1;							
							if (expectedGrids == 0) {
								//got all the grids
								//harmonise the column count across all grids
								int max = 0;
								for (ChartGrid gr: intoList) {
									if (gr.getMaxColumnCount() > max) {
										max = gr.getMaxColumnCount();
									}
								}
								for (ChartGrid gr: intoList) {
									gr.adjustAndDisplay(max);
								}
							}
					}			
		});

	}
	
	int expectedGrids;
	
	private SimplePanel makeGridPanel(String[] compounds) {
		SimplePanel sp = new SimplePanel();
		int h = 180 * compounds.length;
		sp.setHeight(h + "px");
		return sp;
	}
	
	public void redraw(boolean fromUpdate) {

		if (chartSubtypeCombo.getItemCount() == 0 && !fromUpdate) {
			updateSeriesSubtypes(); // will redraw for us later
		} else {

			if (chartSubtypeCombo.getSelectedIndex() == -1) {
				setSubtype((lastSubtype == -1 ? 0 : lastSubtype));				
			}
			
			ivp.clear();								
			final String subtype = chartSubtypeCombo.getItemText(chartSubtypeCombo
									.getSelectedIndex());
			
			final String[] columns = (subtype.equals("All") ? null : new String[] { subtype } );
			
			final List<ChartGrid> grids = new ArrayList<ChartGrid>();
			expectedGrids = 0;
			
			final boolean vsTime = chartCombo.getSelectedIndex() == 0;
			if (groups != null) {
				for (Group g : groups) {
					Label l = new Label("Compounds in '" + g.getName() + "'");
					l.setStyleName("heading");
					ivp.add(l);
					SimplePanel sp = makeGridPanel(g.getCompounds());					
					ivp.add(sp);
					expectedGrids += 1;
					gridFor(vsTime, columns, g.getCompounds(), grids, sp);		
				}
			} else {
				SimplePanel sp = makeGridPanel(compounds.toArray(new String[0]));				
				ivp.add(sp);
				expectedGrids += 1;
				gridFor(vsTime, columns, null, grids, sp);							
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
			setSubtype((lastSubtype == -1 ? chartSubtypeCombo.getItemCount() - 2 : lastSubtype));			
			redraw(true);
		}
	}
	
	private void setType(int type) {
		chartCombo.setSelectedIndex(type);
		if (lastType != type) {
			lastType = type;		
			lastSubtype = -1;
		}
	}
	private void setSubtype(int subtype) {
		chartSubtypeCombo.setSelectedIndex(subtype);
		lastSubtype = subtype;
	}
}
