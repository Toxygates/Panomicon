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

package otgviewer.client.components.groupdef;

import java.util.*;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.Screen;
import otgviewer.client.components.groupdef.SelectionTDGrid.UnitListener;
import t.common.shared.*;
import t.common.shared.sample.SampleClassUtils;
import t.common.shared.sample.Unit;
import t.model.SampleClass;
import t.model.sample.Attribute;

/**
 * A SelectionTDGrid with multiple sections, one for each data filter. Dispatches compound and
 * filter change signals to subgrids appropriately.
 */
public class MultiSelectionGrid extends DataListenerWidget implements SelectionTDGrid.UnitListener {

  private SelectionTDGrid currentGrid;
  private Map<SampleClass, SelectionTDGrid> sections = new HashMap<SampleClass, SelectionTDGrid>();
  private UnitListener listener;
  private VerticalPanel vp;
  private final Screen screen;
  protected final Logger logger = SharedUtils.getLogger("msg");

  public MultiSelectionGrid(Screen scr, @Nullable SelectionTDGrid.UnitListener listener) {
    vp = new VerticalPanel();
    initWidget(vp);
    this.screen = scr;
    this.listener = listener;
  }

  private Unit[] expectedSelection = new Unit[] {}; // waiting for units (grid count)

  private SelectionTDGrid findOrCreateSection(Screen scr, SampleClass sc, boolean noCompounds) {
    SelectionTDGrid g = sections.get(sc);
    if (g == null) {
      g = scr.factory().selectionTDGrid(scr, this);
      g.sampleClassChanged(sc);
      sections.put(sc, g);
      if (!noCompounds) {
        g.compoundsChanged(chosenCompounds);
      }
      Label l = new Label(SampleClassUtils.label(sc, scr.schema()));
      l.addStyleName("selectionGridSectionHeading");
      vp.add(l);
      vp.add(g);
    }
    return g;
  }

  @Override
  public void unitsChanged(DataListenerWidget sender, List<Unit> units) {
    List<Unit> fullSel = fullSelection(true);
    List<Unit> fullSelAll = fullSelection(false);
    if (listener != null) {
      listener.unitsChanged(this, fullSel);
    }
    logger.info("U.Chgd. Size: " + fullSelAll.size() + " expected: " + expectedSelection.length);
    if (fullSelAll.size() == expectedSelection.length && expectedSelection.length > 0) {
      clearEmptySections();
      expectedSelection = new Unit[] {};
    }
  }

  @Override
  public void availableUnitsChanged(DataListenerWidget sender, List<Pair<Unit, Unit>> units) {
    List<Pair<Unit, Unit>> fullAvailability = allAvailable();
    if (listener != null) {
      listener.availableUnitsChanged(this, fullAvailability);
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

  void setAll(boolean state) {
    for (SelectionTDGrid g : sections.values()) {
      g.setAll(false, true);
    }
  }

  @Override
  public void sampleClassChanged(SampleClass sc) {
    SelectionTDGrid g = findOrCreateSection(screen, sc, false);
    if (g != currentGrid) {
      logger.info("SC change " + sc.toString());
      currentGrid = g;
      clearEmptySections();
    }
  }

  @Override
  public void compoundsChanged(List<String> compounds) {
    if (currentGrid != null) {
      currentGrid.compoundsChanged(compounds);
    }
  }

  @SuppressWarnings("deprecation")
  private boolean isMajorParamSharedControl(DataSchema schema, String value) {
    // TODO: stop using deprecated isMajorParamSharedControl
    return schema.isMajorParamSharedControl(value);
  }

  void setSelection(Unit[] selection) {
    logger.info("Set selection: " + selection.length + " units ");
    if (selection.length > 0) {
      logger.info("1st sel: " + selection[0]);
    }
    final DataSchema schema = screen.schema();

    for (SelectionTDGrid g : sections.values()) {
      g.setAll(false, false);
    }
    expectedSelection = selection;

    final Attribute majorParam = screen.schema().majorParameter();
    Map<SampleClass, Set<String>> lcompounds = new HashMap<SampleClass, Set<String>>();
    for (Unit u : selection) {
      SampleClass sc = SampleClassUtils.asMacroClass(u, schema);
      if (!lcompounds.containsKey(sc)) {
        lcompounds.put(sc, new HashSet<String>());
      }
      String majorVal = u.get(majorParam);
      if (majorVal != null && !isMajorParamSharedControl(schema, majorVal)) {
        lcompounds.get(sc).add(majorVal);
      }
    }

    for (SampleClass sc : lcompounds.keySet()) {
      List<String> compounds = new ArrayList<String>(lcompounds.get(sc));
      Collections.sort(compounds);
      SelectionTDGrid g = findOrCreateSection(screen, sc, true);
      g.compoundsChanged(compounds, selection);
    }
  }

  private void clearEmptySections() {
    int count = vp.getWidgetCount();
    for (int i = 1; i < count; i += 2) {
      SelectionTDGrid tg = (SelectionTDGrid) vp.getWidget(i);
      if (tg != currentGrid && tg.getSelectedUnits(true).size() == 0) {
        vp.remove(i);
        vp.remove(i - 1);
        sections.remove(tg.state().sampleClass);
        clearEmptySections();
        return;
      }
    }
  }
}
