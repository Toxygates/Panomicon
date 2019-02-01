/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package otg.viewer.client.charts;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gwt.user.client.ui.*;

import otg.model.sample.OTGAttribute;
import otg.viewer.client.components.OTGScreen;
import t.common.client.components.ItemSelector;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.sample.Attribute;
import t.viewer.client.Utils;
import t.viewer.client.storage.StorageProvider;
import static otg.viewer.client.charts.DataSource.*;

/**
 * A chart grid where the user can interactively choose what kind of charts to display (for example,
 * vs. time or vs. dose, and what particular times or doses to focus on). The grid fetches data
 * dynamically from the underlying data source in response to such choices.
 */
public class AdjustableGrid<D extends Data, DS extends Dataset<D>> extends Composite {
  public static final int TOTAL_WIDTH = 780;

  private ItemSelector<ValueType> valueTypeSel;
  private ListBox chartCombo, chartSubtypeCombo;

  private ExpressionRowSource source;
  private List<String> majorVals;
  private List<String> organisms;
  private List<Group> groups;
  private VerticalPanel vp;
  private VerticalPanel chartsVerticalPanel;
  private OTGScreen screen;
  private Factory<D, DS> factory;
  private int computedWidth;

  private Logger logger = SharedUtils.getLogger("chart");

  private static int lastType = -1;
  private static String lastSubtype = null;
  private List<String> chartSubtypes = new ArrayList<String>();
  
  private String chartsTitle;

  private final DataSchema schema;
  private final StorageProvider storageProvider;

  public AdjustableGrid(Factory<D, DS> factory, ChartParameters params,
      ExpressionRowSource source) {
    this.source = source;
    this.groups = params.groups;
    this.screen = params.screen;
    this.factory = factory;
    chartsTitle = params.title;
    schema = screen.manager().schema();
    storageProvider = screen.getStorage();

    // TODO use schema somehow to handle organism propagation
    organisms = groups.stream().flatMap(g -> g.collect(OTGAttribute.Organism)).distinct()
        .collect(Collectors.toList());

    Attribute majorParam = schema.majorParameter();
    this.majorVals = GroupUtils.collect(groups, majorParam).collect(Collectors.toList());

    vp = Utils.mkVerticalPanel();
    initWidget(vp);
    // vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

    HorizontalPanel hp = Utils.mkHorizontalPanel();
    vp.add(hp);

    hp.addStyleName("colored");
    hp.setWidth("100%");

    HorizontalPanel ihp = Utils.mkHorizontalPanel();
    hp.add(ihp);
    ihp.setSpacing(5);

    valueTypeSel = new ItemSelector<ValueType>() {
      @Override
      protected ValueType[] values() {
        return ValueType.values();
      }
    };
    valueTypeSel.setSelected(params.vt);
    valueTypeSel.listBox().addChangeHandler(e -> {
      computedWidth = 0;
      redraw(false);
    });
    ihp.add(valueTypeSel);

    chartCombo = new ListBox();
    ihp.add(chartCombo);

    String medTitle = schema.mediumParameter().title();
    String minTitle = schema.minorParameter().title();

    chartCombo.addItem("Expression vs " + minTitle + ", fixed " + medTitle + ":");
    chartCombo.addItem("Expression vs " + medTitle + ", fixed " + minTitle + ":");
    setType((lastType == -1 ? 0 : lastType));

    chartSubtypeCombo = new ListBox();
    ihp.add(chartSubtypeCombo);

    chartSubtypeCombo.addChangeHandler(e -> {
      lastSubtype = chartSubtypes.get(chartSubtypeCombo.getSelectedIndex());
      computedWidth = 0;
      redraw(false);
    });

    chartCombo.addChangeHandler(e -> {
      lastType = chartCombo.getSelectedIndex();
      lastSubtype = null;
      computedWidth = 0;
      updateSeriesSubtypes();
    });

    vp.add(Utils.mkEmphLabel(params.title));

    chartsVerticalPanel = Utils.mkVerticalPanel();
    vp.add(chartsVerticalPanel);
    redraw(false);
  }

  public int computedWidth() {
    return computedWidth;
  }

  private List<DataPoint> allPoints = new ArrayList<DataPoint>();

  final private String SELECTION_ALL = "All";

  private double findMinValue() {
    Double min = null;
    for (DataPoint p : allPoints) {
      if (min == null || (p.value < min && p.value != Double.NaN)) {
        min = p.value;
      }
    }
    return min;
  }

  private double findMaxValue() {
    Double max = null;
    for (DataPoint p : allPoints) {
      if (max == null || (p.value > max && p.value != Double.NaN)) {
        max = p.value;
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

    Attribute columnParam = vsMinor ? schema.mediumParameter() : schema.minorParameter();
    String[] preColumns =
        (columns == null ? (vsMinor ? source.mediumVals() : source.minorVals()) : columns);
    final String[] useColumns =
        schema.filterValuesForDisplay(valueTypeSel.value(), columnParam, preColumns);

    SampleMultiFilter smf = new SampleMultiFilter();
    smf.addPermitted(schema.majorParameter(), useMajors);
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

    source.getPointsAsync(valueTypeSel.value(), smf, makeGroupPolicy(),
        new ExpressionRowSource.DataPointAcceptor() {
          @Override
          public void accept(List<DataPoint> points) {
            allPoints.addAll(points);
            DS ct = factory.dataset(points, vsMinor ? source.minorVals() : source.mediumVals(),
                vsMinor, storageProvider);

            ChartGrid<D> grid =
                factory.grid(screen, ct, useMajors == null ? majorVals : Arrays.asList(useMajors),
                    organisms, true, useColumns, !vsMinor, TOTAL_WIDTH);

            intoList.add(grid);
            intoPanel.add(grid);
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
                gr.adjustAndDisplay(new ChartStyle(0, false, null, false), maxCols, minVal, maxVal, chartsTitle);
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

      chartsVerticalPanel.clear();
      final String subtype = chartSubtypeCombo.getItemText(chartSubtypeCombo.getSelectedIndex());

      final String[] columns = (subtype.equals(SELECTION_ALL) ? null : new String[] {subtype});

      final List<ChartGrid<D>> grids = new ArrayList<ChartGrid<D>>();
      expectedGrids = 0;
      allPoints.clear();

      final boolean vsTime = chartCombo.getSelectedIndex() == 0;
      if (groups != null) {
        String[] majorsA = groups.stream().flatMap(g -> SampleClassUtils.getMajors(schema, g))
            .distinct().toArray(String[]::new);
        SimplePanel sp = makeGridPanel(majorsA);
        chartsVerticalPanel.add(sp);
        expectedGrids += 1;
        gridFor(vsTime, columns, majorsA, grids, sp);
      } else {
        // TODO when is this case used? fuse with above?
        SimplePanel sp = makeGridPanel(majorVals.toArray(new String[0]));
        chartsVerticalPanel.add(sp);
        expectedGrids += 1;
        gridFor(vsTime, columns, null, grids, sp);
      }
    }
  }

  /**
   * Find a dose or time that is present in the user-defined sample groups and that can be displayed
   * in these charts.
   */
  private String findPreferredItem(boolean isMed) {
    final Attribute medParam = schema.mediumParameter();
    final Attribute minParam = schema.minorParameter();
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
        final String[] useMeds = schema.filterValuesForDisplay(valueTypeSel.value(),
            schema.mediumParameter(), source.mediumVals());

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
      logger.info("Preferred medium parameter: " + prefItem);
      for (String mv : source.mediumVals()) {
        if (!schema.isControlValue(mv)) {
          chartSubtypeCombo.addItem(mv);
          chartSubtypes.add(mv);
        }

      }
    } else {
      prefItem = findPreferredItem(false);
      logger.info("Preferred minor parameter: " + prefItem);
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
