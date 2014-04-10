package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.client.Utils;
import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.client.charts.google.GVizChartGrid;
import otgviewer.client.components.Screen;
import otgviewer.shared.Barcode;
import otgviewer.shared.Group;
import otgviewer.shared.OTGUtils;
import otgviewer.shared.ValueType;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A chart grid where the user can interactively choose what kind of charts 
 * to display (for example, vs. time or vs. dose, and what particular times or
 * doses to focus on).
 */
public class AdjustableChartGrid extends Composite {
	public static final int TOTAL_WIDTH = 780;
	
	private ListBox chartCombo, chartSubtypeCombo;
	
	private ChartDataSource source;
	private List<String> compounds;
	private List<Group> groups;
	private VerticalPanel vp;
	private VerticalPanel ivp;
	private Screen screen;
	private int computedWidth;
	private ValueType vt;
	
	private static int lastType = -1, lastSubtype = -1;
	
	public AdjustableChartGrid(Screen screen, ChartDataSource source, List<Group> groups, ValueType vt) {
		this.source = source;
		this.groups = groups;
		this.screen = screen;
		this.compounds = Arrays.asList(OTGUtils.compoundsFor(groups));
		this.vt = vt;
		
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
				computedWidth = 0;
				redraw(false);
			}

		});
		chartCombo.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				lastType = chartCombo.getSelectedIndex();
				lastSubtype = -1;
				computedWidth = 0;
				updateSeriesSubtypes();
			}
		});
		
		ivp = Utils.mkVerticalPanel();
		vp.add(ivp);
		redraw(false);
	}
	
	public int computedWidth() {
		return computedWidth;
	}
	
	private List<ChartSample> allSamples = new ArrayList<ChartSample>();
	
	private double findMinValue() {
		Double min = null;
		for (ChartSample s: allSamples) {
			if (min == null || (s.value < min && s.value != Double.NaN)) {
				min = s.value;
			}
		}
		return min;
	}
	
	private double findMaxValue() {
		Double max = null;
		for (ChartSample s: allSamples) {
			if (max == null || (s.value > max && s.value != Double.NaN)) {
				max = s.value;
			}
		}
		return max;
	}
	
	private ColorPolicy makeGroupPolicy() {
		Map<Barcode, String> colors = new HashMap<Barcode, String>();
		for (Group g: groups) {
			for (Barcode b: g.getSamples()) {
				colors.put(b, g.getColor());
			}
		}
		return new ColorPolicy.MapColorPolicy(colors);
	}
	
	private static String[] withoutControl(String[] columns) {
		List<String> r = new ArrayList<String>();
		for (String c: columns) {
			if (!c.equals("Control")) {
				r.add(c);
			}
		}
		return r.toArray(new String[0]);
	}
	
	//vsTime is the vs-time-ness of each individual sub-chart. So the overall grid will be vs. dose 	
	//(in its columns) if each sub-chart is vs.time.
	private void gridFor(final boolean vsTime, final String[] columns, final String[] useCompounds, 
			final List<ChartGrid> intoList, final SimplePanel intoPanel) {
		String[] preColumns = (columns == null ? (vsTime ? source.doses() : source.times()) : columns);
		final String[] useColumns = (vt == ValueType.Folds ? withoutControl(preColumns) : preColumns);		
		
		if (computedWidth == 0) {
			int theoretical = useColumns.length * GVizChartGrid.MAX_WIDTH;
			if (theoretical > TOTAL_WIDTH) {
				computedWidth = TOTAL_WIDTH;
			} else {
				computedWidth = theoretical;
			}
			setWidth(computedWidth + "px");
		}
		
		source.getSamples(useCompounds, useColumns, makeGroupPolicy(),
				new ChartDataSource.SampleAcceptor() {
					@Override
					public void accept(List<ChartSample> samples) {
						allSamples.addAll(samples);
							
						ChartDataset ct = new ChartDataset(samples, samples, 
								vsTime ? source.times() : source.doses(), vsTime);
						
						ChartGrid cg = new GVizChartGrid(screen, ct, groups,
								useCompounds == null ? compounds : Arrays.asList(useCompounds), true,
								useColumns, !vsTime, TOTAL_WIDTH);
						
						intoList.add(cg);
						intoPanel.add(cg);
						intoPanel.setHeight("");

						expectedGrids -= 1;
						if (expectedGrids == 0) {
							double minVal = findMinValue();
							double maxVal = findMaxValue();							
							// got all the grids
							// harmonise the column count across all grids
							int maxCols = 0;
							for (ChartGrid gr : intoList) {
								if (gr.getMaxColumnCount() > maxCols) {
									maxCols = gr.getMaxColumnCount();
								}
							}
							for (ChartGrid gr : intoList) {
								gr.adjustAndDisplay(maxCols, minVal, maxVal);
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
			allSamples.clear();
			
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
