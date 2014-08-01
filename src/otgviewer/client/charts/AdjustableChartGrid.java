package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import otgviewer.client.Utils;
import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.client.charts.google.GVizChartGrid;
import otgviewer.client.components.Screen;
import otgviewer.shared.Group;
import otgviewer.shared.OTGSample;
import otgviewer.shared.OTGUtils;
import otgviewer.shared.ValueType;
import t.common.shared.DataSchema;
import t.common.shared.Unit;

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
	private List<String> majorVals;
	private List<Group> groups;
	private VerticalPanel vp;
	private VerticalPanel ivp;
	private Screen screen;
	private int computedWidth;
	private ValueType vt;
	
	private Logger logger = Utils.getLogger("chart");
	
	private static int lastType = -1;
	private static String lastSubtype = null;
	private List<String> chartSubtypes = new ArrayList<String>();
	
	private final DataSchema schema;
	
	public AdjustableChartGrid(Screen screen, ChartDataSource source, List<Group> groups, ValueType vt) {
		this.source = source;
		this.groups = groups;
		this.screen = screen;
		schema = screen.schema();
		
		String majorParam = screen.schema().majorParameter();
		this.majorVals = 
				new ArrayList<String>(OTGUtils.collect(groups, majorParam));
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
				lastSubtype = chartSubtypes.get(chartSubtypeCombo.getSelectedIndex());
				computedWidth = 0;
				redraw(false);
			}

		});
		chartCombo.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				lastType = chartCombo.getSelectedIndex();
				lastSubtype = null;
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
		Map<OTGSample, String> colors = new HashMap<OTGSample, String>();
		for (Group g: groups) {
			for (OTGSample b: g.getSamples()) {
				colors.put(b, g.getColor());
			}
		}
		return new ColorPolicy.MapColorPolicy(colors);
	}
	
	private String[] withoutControl(String[] columns) {
		List<String> r = new ArrayList<String>();
		for (String c: columns) {
			if (!c.equals("Control")) {
				r.add(c);
			}
		}
		return r.toArray(new String[0]);
	}
	
	//vsMinor is the vs-minor-ness of each individual sub-chart. So the overall grid will be vs. dose 	
	//(in its columns) if each sub-chart is vs.minor.
	private void gridFor(final boolean vsMinor, final String[] columns, final String[] useCompounds, 
			final List<ChartGrid> intoList, final SimplePanel intoPanel) {
		String[] preColumns = (columns == null ? (vsMinor ? source.doses() : source.times()) : columns);
		//TODO
		final String[] useColumns = preColumns; //(vt == ValueType.Folds ? withoutControl(preColumns) : preColumns);		
		
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
								vsMinor ? source.times() : source.doses(), vsMinor);
												
						ChartGrid cg = new GVizChartGrid(screen, ct, groups,
								useCompounds == null ? majorVals : Arrays.asList(useCompounds), true,
								useColumns, !vsMinor, TOTAL_WIDTH);
						
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
				boolean isDose = chartCombo.getSelectedIndex() == 0;
				setSubtype(lastSubtype != null ? lastSubtype : findPreferredItem(isDose));				
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
				SimplePanel sp = makeGridPanel(majorVals.toArray(new String[0]));				
				ivp.add(sp);
				expectedGrids += 1;
				gridFor(vsTime, columns, null, grids, sp);							
			}
			
		}
	}
	
	/**
	 * Find a dose or time that is present in the user-defined sample groups and that
	 * can be displayed in these charts.
	 * @param isMed
	 * @return
	 */
	private String findPreferredItem(boolean isMed) {
		final String medParam = schema.mediumParameter();
		final String minParam = schema.minorParameter();
		if (lastSubtype != null) {
			if (lastSubtype.equals("All")) {
				return lastSubtype;
			}
			// Try to reuse the most recent one
			for (Group g : groups) {
				if (isMed) {
					//TODO
					if (Unit.contains(g.getUnits(), medParam, lastSubtype)) {						
						return lastSubtype;
					}
				} else {
					if (Unit.contains(g.getUnits(), minParam, lastSubtype)) {
						return lastSubtype;
					}
				}
			}
		}
		//Find a new item to use
		for (Unit u: groups.get(0).getUnits()) {
			if (isMed) {
				final String[] useDoses = source.doses();
						//TODO
						//(vt == ValueType.Folds ? withoutControl(source.doses()) : source.doses());
				//TODO
				String dose = u.get(medParam);
				if (Arrays.binarySearch(useDoses, dose) != -1) {
					return dose;
				}
			} else {
				String time = u.get(minParam);
				if (Arrays.binarySearch(source.times(), time) != -1) {
					return time;
				}
			}
		}
		return null;
	}
	
	private void updateSeriesSubtypes() {
		chartSubtypeCombo.clear();		
		chartSubtypes.clear();
		String prefItem;
		if (chartCombo.getSelectedIndex() == 0) {
			prefItem = findPreferredItem(true);
			logger.info("Preferred dose: " + prefItem);			
			for (String dose: source.doses()) {
				chartSubtypeCombo.addItem(dose);
				chartSubtypes.add(dose);
				
			}
		} else {		
			prefItem = findPreferredItem(false);
			logger.info("Preferred time: " + prefItem);			
			for (String time: source.times()) {
				chartSubtypeCombo.addItem(time);
				chartSubtypes.add(time);				
			}
		}
		
		if (chartSubtypeCombo.getItemCount() > 0) {
			chartSubtypeCombo.addItem("All");		
			chartSubtypes.add("All");
			setSubtype(prefItem);			
			redraw(true);
		}
	}
	
	private void setType(int type) {
		chartCombo.setSelectedIndex(type);
		if (lastType != type) {
			lastType = type;		
			lastSubtype = null;
		}
	}
	
	private void setSubtype(String subtype) {
		int idx = -1;
		if (subtype != null) {
			idx = chartSubtypes.indexOf(subtype);
		}
		if (idx != -1) {
			chartSubtypeCombo.setSelectedIndex(idx);
			lastSubtype = subtype;
		} else if (chartSubtypeCombo.getItemCount() > 0) {
			chartSubtypeCombo.setSelectedIndex(0);			
		}
	}	
}
