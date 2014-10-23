package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.Screen;
import otgviewer.client.dialog.DialogPosition;
import otgviewer.shared.Group;
import otgviewer.shared.OTGSample;
import t.common.shared.Pair;
import t.common.shared.SampleClass;
import t.common.shared.Unit;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A time/dose grid for defining and editing sample groups in terms of time/dose
 * combinations for particular compounds.
 * 
 * @author johan
 *
 */
public class SelectionTDGrid extends TimeDoseGrid {

	private CheckBox[] cmpDoseCheckboxes; //selecting all samples for a cmp/dose combo
	private CheckBox[] doseTimeCheckboxes; //selecting all samples for a dose/time combo
	private Unit[] oldSelection;
	
	private Map<Unit, UnitUI> unitUis = new HashMap<Unit, UnitUI>();
	private Map<Unit, Unit> controlUnits = new HashMap<Unit, Unit>();
	
	private class UnitUI extends Composite {
		CheckBox cb = new CheckBox();	
		Unit unit;
		Anchor a;
		Label l;
		UnitUI(Unit u) {			
			Panel p = new HorizontalPanel();
			p.setWidth("3.5em");
			initWidget(p);
			cb.setText("");			
			cb.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					fireUnitsChanged();
				}
			});			
			p.add(cb);
			a = new Anchor("");			
			p.add(a);
			a.setTitle("Treated samples/Control samples");
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
				String controlCount = 
					(controlUnit != null ? controlUnit.getSamples().length + "" : "?");
				a.setText(" " + treatedCount + "/" + controlCount);				
				a.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						displaySampleTable(unit);				
					}			
				});
			} else {
				cb.setEnabled(false);
				l.setText("0/0");
				a.setText("");
				a.setEnabled(false);
			}	
		}
		
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
	
	static interface UnitListener {
		void unitsChanged(DataListenerWidget sender, List<Unit> units);
	}
	
	private UnitListener listener;
	
	public SelectionTDGrid(Screen screen, @Nullable UnitListener listener) {
		super(screen, true);
		this.listener = listener;		
	}
	
	@Override
	public void compoundsChanged(List<String> compounds) {
		oldSelection = getSelectedCombinations();		
		super.compoundsChanged(compounds);		
	}

	public void setAll(boolean val) {
		for (UnitUI ui : unitUis.values()) {
			ui.setValue(val);
		}
		fireUnitsChanged();
	}
	
	protected void setSelected(Unit unit, boolean v) {
		setSelected(unit, v, true);
	}
	
	protected void setSelected(Unit unit, boolean v, boolean fire) {
		UnitUI ui = unitUis.get(unit);
		if (ui != null) {
			ui.setValue(v);
		}
		if (fire) {
			fireUnitsChanged();
		}
	}
	
	private void fireUnitsChanged() {		
		if (listener != null) {
			listener.unitsChanged(this, getSelectedUnits(true));
		}
	}

	protected void setSelection(Unit[] units) {
		setAll(false);
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

	protected Unit[] getSelectedCombinations() {
		List<Unit> r = new ArrayList<Unit>();
		for (Unit u: unitUis.keySet()) {
			UnitUI ui = unitUis.get(u);
			if (ui.getValue()) {
				r.add(u);
			}
		}
		return r.toArray(new Unit[0]);		
	}
	
	private abstract class UnitMultiSelector implements ValueChangeHandler<Boolean> {
		public void onValueChange(ValueChangeEvent<Boolean> vce) {
			for (Unit b: unitUis.keySet()) {
				if (filter(b) && unitUis.get(b).isEnabled()) {
					unitUis.get(b).setValue(vce.getValue());					
				}
			}			
			fireUnitsChanged();
		}
		
		abstract protected boolean filter(Unit b);
	}
	
	//Two parameter constraint
	private class DualSelectHandler extends UnitMultiSelector {
		private String p1, p2, v1, v2;
		
		DualSelectHandler(String p1, String v1, String p2, String v2) {
			this.p1 = p1;
			this.p2 = p2;
			this.v1 = v1;
			this.v2 = v2;			
		}
		
		protected boolean filter(Unit b) {
			return b.get(p1).equals(v1) && b.get(p2).equals(v2);			
		}			
	}
	
	public List<OTGSample> getSelectedBarcodes() {		
		List<OTGSample> r = new ArrayList<OTGSample>();
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
		sc.put(majorParameter, major);
		sc.put(mediumParameter, medium);
		sc.put(minorParameter, minor);
	}

	public List<Unit> getSelectedUnits(boolean treatedOnly) {
		List<Unit> r = new ArrayList<Unit>();
		for (Unit k : unitUis.keySet()) {
			if (unitUis.get(k).getValue()) {
				r.add(k);
				if (!treatedOnly) {
					Unit control = controlUnits.get(k);
					if (control != null) {
						r.add(control);
					}
				}
			}
		}
		return r;
	}
	
	@Override
	protected Widget guiForUnit(final Unit unit) {		
		UnitUI ui = new UnitUI(unit);	
		unitUis.put(unit, ui);
		return ui;		
	}
	
	private void displaySampleTable(Unit unit) {
		SampleDetailTable st = new SampleDetailTable(this, "Experiment detail");
		Unit finalUnit = getFinalUnit(unit);
		if (finalUnit.getSamples() != null && finalUnit.getSamples().length > 0) {
			Unit controlUnit = controlUnits.get(finalUnit);			
			Unit[] units = (controlUnit != null ?
					new Unit[] { finalUnit, controlUnit } :
					new Unit[] { finalUnit });
			Group g = new Group(schema, "data", units);			
			st.loadFrom(g, true, 0, -1);
			Utils.displayInPopup("Unit details", st, DialogPosition.Center);
		} else {
			Window.alert("No samples available for " + unit.toString());
		}
	}
	
	/**
	 * Get the final version of the unit, which is installed
	 * after sample loading.
	 * @param key
	 * @return
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
		CheckBox all = new CheckBox("All");		
		all.setEnabled(false); //disabled by default until samples have been confirmed
		cmpDoseCheckboxes[compound * nd + dose] = all;
		all.addValueChangeHandler(new DualSelectHandler(majorParameter,
				chosenCompounds.get(compound),
				mediumParameter,
				mediumValues.get(dose)));				
		return all;		
	}

	@Override
	protected Widget guiForDoseTime(int dose, int time) {
		Panel p = new HorizontalPanel();
		p.setWidth("4em");
		CheckBox cb = new CheckBox(minorValues.get(time));
		p.add(cb);
		cb.setEnabled(false); //disabled by default until samples have been confirmed
		final int col = dose * minorValues.size() + time;
		doseTimeCheckboxes[col] = cb;
		cb.addValueChangeHandler(new DualSelectHandler(mediumParameter,
				mediumValues.get(dose),
				minorParameter,
				minorValues.get(time)));
		return p;
	}

	@Override
	protected void drawGridInner(Grid grid) {		
		final int nd = mediumValues.size();
		cmpDoseCheckboxes = new CheckBox[chosenCompounds.size() * nd];
		doseTimeCheckboxes = new CheckBox[mediumValues.size() * minorValues.size()];
		unitUis.clear();
		
		super.drawGridInner(grid);
	}	
	
	private String unitString(Unit u) {
		if (u == null) {
			return "(null)";
		} else if (u.getSamples() == null || u.getSamples().length == 0) {
			return "(no samples)";		
		} else {
			return u.getSamples().length + " sampl: " + u.getSamples()[0].getCode();
		}
	}
	
	@Override 
	protected void samplesAvailable() {
		logger.info("Samples available: " + availableUnits.length + " units");
		for (Pair<Unit, Unit> u: availableUnits) {		
			controlUnits.put(u.first(), u.second());			
			logger.info("treated: " + unitString(u.first()) + " control: " + unitString(u.second()));												
		}
		
		for (Pair<Unit, Unit> treatedControl: availableUnits) {
			Unit u = treatedControl.first();
			if (u == null || u.getSamples() == null || u.getSamples().length == 0) {				
				continue;
			}
			
			UnitUI ui = unitUis.get(u);			
			if (ui == null) {											
				continue;
			}
			unitUis.remove(u);
			ui.setUnit(u);
			// Remove the key and replace it since the ones from availableUnits
			// will be populated with concrete Barcodes (getSamples)
			unitUis.put(u, ui);
			
			int cIdx = chosenCompounds.indexOf(u.get(majorParameter));
			int dIdx = mediumValues.indexOf(u.get(mediumParameter));
			int tIdx = minorValues.indexOf(u.get(minorParameter));
			
			if (cIdx == -1 || dIdx == -1 || tIdx == -1) {
				Window.alert("Data error");
				return;
			}
			
			cmpDoseCheckboxes[cIdx * mediumValues.size() + dIdx].setEnabled(true);
			doseTimeCheckboxes[dIdx * minorValues.size() + tIdx]
					.setEnabled(true);

		}
		if (oldSelection != null) {
			setSelection(oldSelection);
			oldSelection = null;
		}	
	}
}
