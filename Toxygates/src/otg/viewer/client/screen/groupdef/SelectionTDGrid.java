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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otg.viewer.client.SampleDetailTable;
import otg.viewer.client.components.OTGScreen;
import otg.viewer.client.components.TimeDoseGrid;
import t.common.shared.Pair;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.future.Future;

/**
 * A time/dose grid for defining and editing sample groups in terms of time/dose combinations for
 * particular compounds.
 */
abstract public class SelectionTDGrid extends TimeDoseGrid {

  /*
   * Note: like many other classes in otg.viewer, this is probably too general 
   * and could be moved to t.viewer
   */
  
  private CheckBox[] cmpDoseCheckboxes; // selecting all samples for a cmp/dose combo
  private CheckBox[] doseTimeCheckboxes; // selecting all samples for a dose/time combo

  private Map<Unit, UnitUI> unitUis = new HashMap<Unit, UnitUI>();
  private Map<Unit, Unit> controlUnits = new HashMap<Unit, Unit>();
  
  public SampleClass sampleClass() {
    return chosenSampleClass.copy();
  }

  protected abstract class UnitUI extends Composite {
    CheckBox cb = new CheckBox();
    Unit unit;
    Anchor a;
    Label l;

    UnitUI(Unit u) {
      Panel p = new HorizontalPanel();
      p.addStyleName("unitGui");
      initWidget(p);
      cb.setText("");
      cb.addClickHandler(e -> fireUnitsChanged());        
      p.add(cb);
      a = new Anchor("");
      p.add(a);
      a.setTitle(unitHoverText());
      l = new Label("(?)");
      p.add(l);
      setUnit(u);
    }

    void setUnit(final Unit u) {
      this.unit = u;
      if (unit.getSamples() != null && unit.getSamples().length > 0) {
        cb.setEnabled(true);
        l.setText("");
        a.setEnabled(true);
        int treatedCount = unit.getSamples().length;
        Unit controlUnit = controlUnits.get(unit);
        int controlCount = (controlUnit != null ? controlUnit.getSamples().length : 0);
        a.setText(unitLabel(treatedCount, controlCount));
        a.addClickHandler(e -> displaySampleTable(unit));          
      } else {
        cb.setEnabled(false);
        l.setText(unitLabel(0, 0));
        a.setText("");
        a.setEnabled(false);
      }
    }

    protected abstract String unitHoverText();

    protected abstract String unitLabel(int treatedCount, int controlCount);

    void setValue(boolean val) {
      cb.setValue(val);
    }

    boolean getValue() {
      return cb.getValue();
    }

    boolean isEnabled() {
      return cb.isEnabled();
    }
  }

  protected abstract UnitUI makeUnitUI(final Unit unit);

  public SelectionTDGrid(OTGScreen screen) {
    super(screen, true);
  }
  
  public Future<Pair<String[], Pair<Unit, Unit>[]>> initializeState(
      SampleClass sampleClass, List<String> compounds, Unit[] unitSelection,
      boolean datasetsChanged) {
    Future<Pair<String[], Pair<Unit, Unit>[]>> future = 
        super.initializeState(sampleClass, compounds, datasetsChanged);
    if (!compounds.isEmpty()) {
      future.addSuccessCallback(r -> {
        samplesAvailable(unitSelection);
      });
    }    
    return future;
  }

  @Override
  public Future<Pair<Unit, Unit>[]> setCompounds(List<String> compounds) {
    Future<Pair<Unit, Unit>[]> future = super.setCompounds(compounds);
    if (!compounds.isEmpty()) {
      Unit[] selection = getSelectedCombinations();
      future.addSuccessCallback(r -> {
        samplesAvailable(selection);
      });
    }
    return future;
  }

  public void setAll(boolean val, boolean fire) {
    for (UnitUI ui : unitUis.values()) {
      ui.setValue(val);
    }
    if (fire) {
      fireUnitsChanged();
    }
  }

  protected void setSelected(Unit unit, boolean v, boolean fire) {
    UnitUI ui = unitUis.get(unit);
    if (ui != null) {
      ui.setValue(v);
    } else {
      logger.info("Unable to set selection " + unit + " (missing ui)");
    }
    if (fire) {
      fireUnitsChanged();
    }
  }

  private void fireUnitsChanged() {
    
  }

  public void setSelection(Unit[] units) {
    setAll(false, false);
    for (Unit u : units) {
      if (!schema.isSelectionControl(u)) {
        setSelected(u, true, false);
      }
    }
    fireUnitsChanged();
  }

  protected boolean getSelected(Unit unit) {
    return unitUis.get(unit).getValue();
  }

  public Unit[] getSelectedCombinations() {
    List<Unit> r = new ArrayList<Unit>();
    for (Unit u : unitUis.keySet()) {
      UnitUI ui = unitUis.get(u);
      if (ui.getValue()) {
        r.add(u);
      }
    }
    return r.toArray(new Unit[0]);
  }

  private abstract class UnitMultiSelector implements ValueChangeHandler<Boolean> {
    @Override
    public void onValueChange(ValueChangeEvent<Boolean> vce) {
      for (Unit b : unitUis.keySet()) {
        if (filter(b) && unitUis.get(b).isEnabled()) {
          unitUis.get(b).setValue(vce.getValue());
        }
      }
      fireUnitsChanged();
    }

    abstract protected boolean filter(Unit b);
  }

  // Two parameter constraint
  private class DualSelectHandler extends UnitMultiSelector {
    private Attribute p1, p2;
    private String v1, v2;

    DualSelectHandler(Attribute p1, String v1, Attribute p2, String v2) {
      this.p1 = p1;
      this.p2 = p2;
      this.v1 = v1;
      this.v2 = v2;
    }

    @Override
    protected boolean filter(Unit b) {
      return b.get(p1).equals(v1) && b.get(p2).equals(v2);
    }
  }

  public List<Sample> getSelectedBarcodes() {
    List<Sample> r = new ArrayList<Sample>();
    for (Unit k : unitUis.keySet()) {
      if (unitUis.get(k).getValue()) {
        r.addAll(Arrays.asList(k.getSamples()));
      }
    }
    if (r.isEmpty()) {
      Window.alert("Please select at least one time/dose combination.");
    }
    return r;
  }

  protected void makeSampleClass(String major, String medium, String minor) {
    SampleClass sc = new SampleClass();
    sc.put(schema.majorParameter(), major);
    sc.put(schema.mediumParameter(), medium);
    sc.put(schema.minorParameter(), minor);
  }

  public List<Unit> getSelectedUnits(boolean treatedOnly) {
    List<Unit> r = new ArrayList<Unit>();
    HashSet<Unit> alreadyAddedControls = new HashSet<Unit>();
    for (Unit k : unitUis.keySet()) {
      if (unitUis.get(k).getValue()) {
        r.add(k);
        if (!treatedOnly) {
          Unit control = controlUnits.get(k);
          if (control != null && !alreadyAddedControls.contains(control)) {
            r.add(control);
            alreadyAddedControls.add(control);
          }
        }
      }
    }
    return r;
  }

  public List<Pair<Unit, Unit>> getAvailableUnits() {
    List<Pair<Unit, Unit>> r = new ArrayList<Pair<Unit, Unit>>();
    for (Unit k : controlUnits.keySet()) {
      Unit control = controlUnits.get(k);
      if (control != null) {
        Pair<Unit, Unit> p = new Pair<Unit, Unit>(k, control);
        r.add(p);
      }
    }
    return r;
  }

  @Override
  protected Widget guiForUnit(final Unit unit) {
    UnitUI ui = makeUnitUI(unit);
    unitUis.put(unit, ui);
    return ui;
  }

  private void displaySampleTable(Unit unit) {
    SampleDetailTable st = new SampleDetailTable(screen, "Experiment detail", false);
    Unit finalUnit = getFinalUnit(unit);
    if (finalUnit.getSamples() != null && finalUnit.getSamples().length > 0) {
      Unit controlUnit = controlUnits.get(finalUnit);
      Unit[] units =
          (controlUnit != null ? new Unit[] {finalUnit, controlUnit} : new Unit[] {finalUnit});
      Group g = new Group(schema, "data", units);
      st.loadFrom(g, true);
      Utils.displayInPopup("Unit details", st, DialogPosition.Center);
    } else {
      Window.alert("No samples available for " + unit.toString());
    }
  }

  /**
   * Get the final version of the unit, which is installed after sample loading.
   */
  private Unit getFinalUnit(Unit key) {
    for (Unit b : unitUis.keySet()) {
      if (b.equals(key)) {
        return b;
      }
    }
    return null;
  }

  @Override
  protected Widget guiForCompoundDose(int compound, int dose) {
    final int nd = mediumValues.size();
    Panel panel = new HorizontalPanel();
    CheckBox checkBox = new CheckBox();
    checkBox.setEnabled(false); // disabled by default until samples have been confirmed
    cmpDoseCheckboxes[compound * nd + dose] = checkBox;
    checkBox.addValueChangeHandler(new DualSelectHandler(schema.majorParameter(),
        chosenCompounds.get(compound), schema.mediumParameter(), mediumValues.get(dose)));
    panel.add(checkBox);
    panel.add(new Label(" All"));
    panel.addStyleName("compoundDoseGui");
    return panel;
  }

  @Override
  protected Widget guiForDoseTime(int dose, int time) {
    Panel p = new HorizontalPanel();
    p.setWidth("4em");
    CheckBox cb = new CheckBox(minorValues.get(time));
    p.add(cb);
    cb.setEnabled(false); // disabled by default until samples have been confirmed
    final int col = dose * minorValues.size() + time;
    doseTimeCheckboxes[col] = cb;
    cb.addValueChangeHandler(new DualSelectHandler(schema.mediumParameter(), mediumValues.get(dose),
        schema.minorParameter(), minorValues.get(time)));
    return p;
  }

  @Override
  synchronized protected void drawGridInner(Grid grid) {
    final int nd = mediumValues.size();
    cmpDoseCheckboxes = new CheckBox[chosenCompounds.size() * nd];
    doseTimeCheckboxes = new CheckBox[mediumValues.size() * minorValues.size()];
    unitUis.clear();

    super.drawGridInner(grid);
  }
  
  protected void samplesAvailable(Unit[] selection) {
    //logger.info("Samples available: " + availableUnits.size() + " units.");
//    if (availableUnits.size() > 0 && availableUnits.get(0) != null) {
//      logger.info("1st: " + availableUnits.get(0));
//    }
    
    controlUnits.clear();
    for (Pair<Unit, Unit> u : availableUnits) {
      controlUnits.put(u.first(), u.second());
//      logger.info("treated: " + u.first() + " control: " + u.second());
    }

    for (Pair<Unit, Unit> treatedControl : availableUnits) {
      Unit u = treatedControl.first();
      if (u == null || u.getSamples() == null || u.getSamples().length == 0) {
//        if (u != null) {
//          logger.warning("Not creating UI for unit " + u);
//        }
        continue;
      }

      UnitUI ui = unitUis.get(u);
      if (ui == null) {
//        logger.warning("No UI for unit " + u);
        continue;
      }
      unitUis.remove(u);
      ui.setUnit(u);
      // Remove the key and replace it since the ones from availableUnits
      // will be populated with concrete Barcodes (getSamples)
      unitUis.put(u, ui);
      
      int cIdx = chosenCompounds.indexOf(u.get(schema.majorParameter()));
      int dIdx = mediumValues.indexOf(u.get(schema.mediumParameter()));
      int tIdx = minorValues.indexOf(u.get(schema.minorParameter()));

      if (cIdx == -1 || dIdx == -1 || tIdx == -1) {
        Window.alert("Data error");
        return;
      }

      cmpDoseCheckboxes[cIdx * mediumValues.size() + dIdx].setEnabled(true);
      doseTimeCheckboxes[dIdx * minorValues.size() + tIdx].setEnabled(true);
    }
    setSelection(selection);

  }
}
