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

package otg.viewer.client.screen.groupdef;

import java.util.*;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.*;

import otg.viewer.client.components.OTGScreen;
import otg.viewer.client.screen.groupdef.SelectionTDGrid.UnitListener;
import t.common.shared.*;
import t.common.shared.sample.SampleClassUtils;
import t.common.shared.sample.Unit;
import t.model.SampleClass;
import t.model.sample.Attribute;

/**
 * A SelectionTDGrid with multiple sections, one for each data filter. Dispatches compound and
 * filter change signals to subgrids appropriately.
 */
public class MultiSelectionGrid extends Composite implements SelectionTDGrid.UnitListener {

  private SelectionTDGrid currentGrid;
  private Map<SampleClass, SelectionTDGrid> sections = new HashMap<SampleClass, SelectionTDGrid>();
  private UnitListener listener;
  private VerticalPanel verticalPanel;
  private final OTGScreen screen;
  protected final Logger logger = SharedUtils.getLogger("msg");

  protected List<String> chosenCompounds = new ArrayList<String>();

  public MultiSelectionGrid(OTGScreen screen, @Nullable SelectionTDGrid.UnitListener listener) {
    verticalPanel = new VerticalPanel();
    initWidget(verticalPanel);
    this.screen = screen;
    this.listener = listener;
  }

  private Unit[] expectedSelection = new Unit[] {}; // waiting for units (grid count)

  private SelectionTDGrid findOrCreateSection(SampleClass sampleClass, boolean noCompounds) {
    SelectionTDGrid grid = sections.get(sampleClass);
    if (grid == null) {
      grid = screen.factory().selectionTDGrid(screen, this);
      grid.sampleClassChanged(sampleClass);
      sections.put(sampleClass, grid);
      if (!noCompounds) {
        grid.compoundsChanged(chosenCompounds);
      }
      Label label = new Label(SampleClassUtils.label(sampleClass, screen.schema()));
      label.addStyleName("selectionGridSectionHeading");
      verticalPanel.add(label);
      verticalPanel.add(grid);
    }
    return grid;
  }

  @Override
  public void unitsChanged(List<Unit> units) {
    List<Unit> fullSel = fullSelection(true);
    List<Unit> fullSelAll = fullSelection(false);
    if (listener != null) {
      listener.unitsChanged(fullSel);
    }
    //logger.info("U.Chgd. Size: " + fullSelAll.size() + " expected: " + expectedSelection.length);
    if (fullSelAll.size() == expectedSelection.length && expectedSelection.length > 0) {
      clearEmptySections();
      expectedSelection = new Unit[] {};
    }
  }

  @Override
  public void availableUnitsChanged(List<Pair<Unit, Unit>> units) {
    List<Pair<Unit, Unit>> fullAvailability = allAvailable();
    if (listener != null) {
      listener.availableUnitsChanged(fullAvailability);
    }
  }

  List<Unit> fullSelection(boolean treatedOnly) {
    List<Unit> r = new ArrayList<Unit>();
    for (SelectionTDGrid g : sections.values()) {
      r.addAll(g.getSelectedUnits(treatedOnly));
    }
    return r;
  }

  List<Pair<Unit, Unit>> allAvailable() {
    List<Pair<Unit, Unit>> r = new ArrayList<Pair<Unit, Unit>>();
    for (SelectionTDGrid g : sections.values()) {
      r.addAll(g.getAvailableUnits());
    }
    return r;
  }

  void clearSelection() {
    for (SelectionTDGrid g : sections.values()) {
      g.setAll(false, true);
    }
  }

  public void sampleClassChanged(SampleClass sc) {
    SelectionTDGrid g = findOrCreateSection(sc, false);
    if (g != currentGrid) {
      currentGrid = g;
      clearEmptySections();
    }
  }

  public void compoundsChanged(List<String> compounds) {
    chosenCompounds = compounds;
    currentGrid.compoundsChanged(compounds);
  }

  @SuppressWarnings("deprecation")
  private boolean isMajorParamSharedControl(DataSchema schema, String value) {
    // TODO: stop using deprecated isMajorParamSharedControl
    return schema.isMajorParamSharedControl(value);
  }

  void setSelection(Unit[] selection) {
    logger.info("Set selection: " + selection.length + " units" + 
        (selection.length > 0 ? ("\n1st selection: " + selection[0]) : ""));
    final DataSchema schema = screen.schema();

    for (SelectionTDGrid grid : sections.values()) {
      grid.setAll(false, false);
    }
    expectedSelection = selection;

    final Attribute majorParam = screen.schema().majorParameter();
    Map<SampleClass, Set<String>> lcompounds = new HashMap<SampleClass, Set<String>>();
    for (Unit unit : selection) {
      SampleClass sc = SampleClassUtils.asMacroClass(unit, schema);
      if (!lcompounds.containsKey(sc)) {
        lcompounds.put(sc, new HashSet<String>());
      }
      String majorVal = unit.get(majorParam);
      if (majorVal != null && !isMajorParamSharedControl(schema, majorVal)) {
        lcompounds.get(sc).add(majorVal);
      }
    }

    for (SampleClass sc : lcompounds.keySet()) {
      List<String> compounds = new ArrayList<String>(lcompounds.get(sc));
      Collections.sort(compounds);
      SelectionTDGrid g = findOrCreateSection(sc, true);
      g.compoundsChanged(compounds, selection);
    }
  }

  private void clearEmptySections() {
    int count = verticalPanel.getWidgetCount();
    for (int i = 1; i < count; i += 2) {
      SelectionTDGrid tg = (SelectionTDGrid) verticalPanel.getWidget(i);
      if (tg != currentGrid && tg.getSelectedUnits(true).size() == 0) {
        verticalPanel.remove(i);
        verticalPanel.remove(i - 1);
        sections.remove(tg.sampleClass());
        clearEmptySections();
        return;
      }
    }
  }
}