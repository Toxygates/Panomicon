/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client.charts;

import java.util.*;
import java.util.logging.Logger;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.Screen;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.viewer.client.Utils;

/**
 * A chart grid where the user can interactively choose what kind of charts to display (for example,
 * vs. time or vs. dose, and what particular times or doses to focus on).
 */
public class AdjustableGrid<D extends Data, DS extends Dataset<D>> extends Composite {
  public static final int TOTAL_WIDTH = 780;

  private ListBox chartCombo, chartSubtypeCombo;

  private DataSource source;
  private List<String> majorVals;
  private List<String> organisms;
  private List<Group> groups;
  private VerticalPanel vp;
  private VerticalPanel ivp;
  private Screen screen;
  private Factory<D, DS> factory;
  private int computedWidth;
  private ValueType valueType;

  private Logger logger = SharedUtils.getLogger("chart");

  private static int lastType = -1;
  private static String lastSubtype = null;
  private List<String> chartSubtypes = new ArrayList<String>();

  private final DataSchema schema;

  public AdjustableGrid(Factory<D, DS> factory, Screen screen, DataSource source,
      List<Group> groups, ValueType vt) {
    this.source = source;
    this.groups = groups;
    this.screen = screen;
    this.factory = factory;
    schema = screen.schema();

    Set<String> os = new HashSet<String>();

    // TODO use schema somehow to handle organism propagation
    for (Group g : groups) {
      os.addAll(g.collect("organism"));
    }
    organisms = new ArrayList<String>(os);

    String majorParam = screen.schema().majorParameter().id();
    this.majorVals = new ArrayList<String>(GroupUtils.collect(groups, majorParam));
    this.valueType = vt;

    vp = Utils.mkVerticalPanel();
    initWidget(vp);
    // vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

    HorizontalPanel hp = Utils.mkHorizontalPanel();
    vp.add(hp);

    hp.setStylePrimaryName("colored");
    hp.setWidth("100%");

    HorizontalPanel ihp = Utils.mkHorizontalPanel();
    hp.add(ihp);
    ihp.setSpacing(5);

    chartCombo = new ListBox();
    ihp.add(chartCombo);

    String medTitle = schema.mediumParameter().title();
    String minTitle = schema.minorParameter().title();

    chartCombo.addItem("Expression vs " + minTitle + ", fixed " + medTitle + ":");
    chartCombo.addItem("Expression vs " + medTitle + ", fixed " + minTitle + ":");
    setType((lastType == -1 ? 0 : lastType));

    chartSubtypeCombo = new ListBox();
    ihp.add(chartSubtypeCombo);

    chartSubtypeCombo.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        lastSubtype = chartSubtypes.get(chartSubtypeCombo.getSelectedIndex());
        computedWidth = 0;
        redraw(false);
      }

    });
    chartCombo.addChangeHandler(new ChangeHandler() {
      @Override
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

  final private String SELECTION_ALL = "All";

  private double findMinValue() {
    Double min = null;
    for (ChartSample s : allSamples) {
      if (min == null || (s.value < min && s.value != Double.NaN)) {
        min = s.value;
      }
    }
    return min;
  }

  private double findMaxValue() {
    Double max = null;
    for (ChartSample s : allSamples) {
      if (max == null || (s.value > max && s.value != Double.NaN)) {
        max = s.value;
      }
    }
    return max;
  }

  private ColorPolicy makeGroupPolicy() {
    Map<Sample, String> colors = new HashMap<Sample, String>();
    for (Group g : groups) {
      for (Sample b : g.getSamples()) {
        colors.put(b, g.getColor());
      }
    }
    return new ColorPolicy.MapColorPolicy(colors);
  }

  // vsMinor is the vs-minor-ness of each individual sub-chart. So the overall grid will
  // be vs. dose in its columns) if each sub-chart is vs.minor.
  private void gridFor(final boolean vsMinor, final String[] columns, final String[] useMajors,
      final List<ChartGrid<D>> intoList, final SimplePanel intoPanel) {

    String columnParam = vsMinor ? schema.mediumParameter().id() : schema.minorParameter().id();
    String[] preColumns =
        (columns == null ? (vsMinor ? source.mediumVals() : source.minorVals()) : columns);
    final String[] useColumns = schema.filterValuesForDisplay(valueType, columnParam, preColumns);

    SampleMultiFilter smf = new SampleMultiFilter();
    smf.addPermitted(schema.majorParameter().id(), useMajors);
    smf.addPermitted(columnParam, useColumns);

    if (computedWidth == 0) {
      int theoretical = useColumns.length * factory.gridMaxWidth();
      if (theoretical > TOTAL_WIDTH) {
        computedWidth = TOTAL_WIDTH;
      } else {
        computedWidth = theoretical;
      }
      setWidth(computedWidth + "px");
    }

    source.getSamples(smf, makeGroupPolicy(), new DataSource.SampleAcceptor() {
      @Override
      public void accept(List<ChartSample> samples) {
        allSamples.addAll(samples);
        DS ct =
            factory.dataset(samples, samples, vsMinor ? source.minorVals() : source.mediumVals(),
                vsMinor);

        ChartGrid<D> cg =
            factory.grid(screen, ct, useMajors == null ? majorVals : Arrays.asList(useMajors),
                organisms, true, useColumns, !vsMinor, TOTAL_WIDTH);

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
          for (ChartGrid<D> gr : intoList) {
            if (gr.getMaxColumnCount() > maxCols) {
              maxCols = gr.getMaxColumnCount();
            }
          }
          for (ChartGrid<D> gr : intoList) {
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
        boolean medVsMin = chartCombo.getSelectedIndex() == 0;
        setSubtype(lastSubtype != null ? lastSubtype : findPreferredItem(medVsMin));
      }

      ivp.clear();
      final String subtype = chartSubtypeCombo.getItemText(chartSubtypeCombo.getSelectedIndex());

      final String[] columns = (subtype.equals(SELECTION_ALL) ? null : new String[] {subtype});

      final List<ChartGrid<D>> grids = new ArrayList<ChartGrid<D>>();
      expectedGrids = 0;
      allSamples.clear();

      final boolean vsTime = chartCombo.getSelectedIndex() == 0;
      if (groups != null) {
        Set<String> majors = new HashSet<String>();
        for (Group g : groups) {
          majors.addAll(SampleClassUtils.getMajors(schema, g));
        }
        String[] majorsA = majors.toArray(new String[0]);
        SimplePanel sp = makeGridPanel(majorsA);
        ivp.add(sp);
        expectedGrids += 1;
        gridFor(vsTime, columns, majorsA, grids, sp);
      } else {
        // TODO when is this case used? fuse with above?
        SimplePanel sp = makeGridPanel(majorVals.toArray(new String[0]));
        ivp.add(sp);
        expectedGrids += 1;
        gridFor(vsTime, columns, null, grids, sp);
      }
    }
  }

  /**
   * Find a dose or time that is present in the user-defined sample groups and that can be displayed
   * in these charts.
   * 
   * @param isMed
   * @return
   */
  private String findPreferredItem(boolean isMed) {
    final String medParam = schema.mediumParameter().id();
    final String minParam = schema.minorParameter().id();
    if (lastSubtype != null) {
      if (lastSubtype.equals(SELECTION_ALL)) {
        return lastSubtype;
      }
      // Try to reuse the most recent one
      for (Group g : groups) {
        if (isMed) {
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
    // Find a new item to use
    for (Unit u : groups.get(0).getUnits()) {
      logger.info("Unit: " + u);
      if (isMed) {
        final String[] useMeds =
            schema.filterValuesForDisplay(valueType, schema.mediumParameter().id(),
                source.mediumVals());

        String med = u.get(medParam);
        if (Arrays.binarySearch(useMeds, med) != -1) {
          return med;
        }
      } else {
        String min = u.get(minParam);
        if (Arrays.binarySearch(source.minorVals(), min) != -1) {
          return min;
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
      logger.info("Preferred medium: " + prefItem);
      for (String mv : source.mediumVals()) {
        if (!schema.isControlValue(mv)) {
          // TODO for NI values in OTG
          chartSubtypeCombo.addItem(mv);
          chartSubtypes.add(mv);
        }

      }
    } else {
      prefItem = findPreferredItem(false);
      logger.info("Preferred minor: " + prefItem);
      for (String minv : source.minorVals()) {
        chartSubtypeCombo.addItem(minv);
        chartSubtypes.add(minv);
      }
    }

    if (chartSubtypeCombo.getItemCount() > 0) {
      chartSubtypeCombo.addItem(SELECTION_ALL);
      chartSubtypes.add(SELECTION_ALL);
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
