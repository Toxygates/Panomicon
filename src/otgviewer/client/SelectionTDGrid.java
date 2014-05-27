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
import otgviewer.shared.BUnit;
import otgviewer.shared.Barcode;
import otgviewer.shared.Group;
import bioweb.shared.SharedUtils;

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
	private BUnit[] oldSelection;
	
	private Map<BUnit, UnitUI> unitUis = new HashMap<BUnit, UnitUI>();
	private Map<String, BUnit> controlUnits = new HashMap<String, BUnit>();
	
	private class UnitUI extends Composite {
		CheckBox cb = new CheckBox();	
		BUnit unit;
		Anchor a;
		Label l;
		UnitUI(BUnit u) {			
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
		
		void setUnit(final BUnit u) {
			this.unit = u;
			if (unit.getSamples() != null && unit.getSamples().length > 0) {
				cb.setEnabled(true);
				l.setText("");
				a.setEnabled(true);
				int treatedCount = unit.getSamples().length;
				int controlCount = controlUnitFor(unit).getSamples().length;
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
		void unitsChanged(DataListenerWidget sender, List<BUnit> units);
	}
	
	private UnitListener listener;
	
	public SelectionTDGrid(Screen screen,  
			@Nullable UnitListener listener) {
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
	
	protected void setSelected(BUnit unit, boolean v) {
		setSelected(unit, v, true);
	}
	
	protected void setSelected(BUnit unit, boolean v, boolean fire) {
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

	protected void setSelection(BUnit[] units) {
		setAll(false);
		for (BUnit u : units) {
			if (!u.getDose().equals("Control")) {		
				setSelected(u, true, false);
			}
		}
		fireUnitsChanged();
	}
	
	protected boolean getSelected(BUnit unit) {
		return unitUis.get(unit).getValue();
	}

	protected BUnit[] getSelectedCombinations() {
		List<BUnit> r = new ArrayList<BUnit>();
		for (BUnit u: unitUis.keySet()) {
			UnitUI ui = unitUis.get(u);
			if (ui.getValue()) {
				r.add(u);
			}
		}
		return r.toArray(new BUnit[0]);		
	}
//	
//	public void setSelection(Barcode[] barcodes) {
//		setAll(false);
//		for (Barcode b: barcodes) {
//			if (!b.getDose().equals("Control")) {
//				setSelected(new BUnit(b), true);
//			}
//		}		
//	}
	
	private abstract class UnitMultiSelector implements ValueChangeHandler<Boolean> {
		public void onValueChange(ValueChangeEvent<Boolean> vce) {
			for (BUnit b: unitUis.keySet()) {
				if (filter(b) && unitUis.get(b).isEnabled()) {
					unitUis.get(b).setValue(vce.getValue());					
				}
			}			
			fireUnitsChanged();
		}
		
		abstract protected boolean filter(BUnit b);
	}
	
	private class CmpDoseSelectHandler extends UnitMultiSelector {
		private String compound;
		private String dose;
		CmpDoseSelectHandler(String compound, String dose) {
			this.compound = compound;
			this.dose = dose;
		}
		
		protected boolean filter(BUnit b) {
			return b.getCompound().equals(compound) &&
					b.getDose().equals(dose);
		}			
	}
	
	private class DoseTimeSelectHandler extends UnitMultiSelector {
		private String dose;
		private String time;
		DoseTimeSelectHandler(String dose, String time) {
			this.dose = dose;
			this.time = time;
		}
		
		protected boolean filter(BUnit b) {
			return b.getTime().equals(time) &&
					b.getDose().equals(dose);
		}
	
	}
	
	public List<Barcode> getSelectedBarcodes() {		
		List<Barcode> r = new ArrayList<Barcode>();
		for (BUnit k : unitUis.keySet()) {
			if (unitUis.get(k).getValue()) {
				r.addAll(Arrays.asList(k.getSamples()));
			}
		}
		if (r.isEmpty()) {
			Window.alert("Please select at least one time/dose combination.");
		}		
		return r;
	}
	
	private BUnit controlUnitFor(BUnit u) {
		BUnit b = new BUnit(u.getCompound(), "Control", u.getTime());
		b.setDataFilter(chosenDataFilter);
		return controlUnits.get(b.toString());
	}
	
	public List<BUnit> getSelectedUnits(boolean treatedOnly) {
		List<BUnit> r = new ArrayList<BUnit>();
		for (BUnit k : unitUis.keySet()) {
			if (unitUis.get(k).getValue()) {
				r.add(k);
				if (!treatedOnly) {
					BUnit control = controlUnitFor(k);
					if (control != null) {
						r.add(control);
					}
				}
			}
		}
		return r;
	}
	
	@Override
	protected Widget guiForUnit(final BUnit unit) {		
		UnitUI ui = new UnitUI(unit);	
		unitUis.put(unit, ui);
		return ui;		
	}
	
	private void displaySampleTable(BUnit unit) {
		SampleDetailTable st = new SampleDetailTable(this, "Experiment detail");
		BUnit finalUnit = getFinalUnit(unit);
		if (finalUnit.getSamples() != null && finalUnit.getSamples().length > 0) {
			BUnit controlUnit = controlUnitFor(finalUnit);
			BUnit[] units = new BUnit[] { finalUnit, controlUnit };
			Group g = new Group("data", units);			
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
	private BUnit getFinalUnit(BUnit key) {
		for (BUnit b : unitUis.keySet()) {
			if (b.equals(key)) {
				return b;
			}
		}
		return null;
	}

	@Override
	protected Widget guiForCompoundDose(int compound, int dose) {
		final int nd = numDoses();
		CheckBox all = new CheckBox("All");		
		all.setEnabled(false); //disabled by default until samples have been confirmed
		cmpDoseCheckboxes[compound * nd + dose] = all;
		all.addValueChangeHandler(new CmpDoseSelectHandler(chosenCompounds.get(compound),
				indexToDose(dose)));				
		return all;		
	}

	@Override
	protected Widget guiForDoseTime(int dose, int time) {
		Panel p = new HorizontalPanel();
		p.setWidth("4em");
		CheckBox cb = new CheckBox(availableTimes[time]);
		p.add(cb);
		cb.setEnabled(false); //disabled by default until samples have been confirmed
		final int col = dose * availableTimes.length + time;
		doseTimeCheckboxes[col] = cb;
		cb.addValueChangeHandler(new DoseTimeSelectHandler(indexToDose(dose),
				availableTimes[time]));
		return p;
	}

	@Override
	protected void drawGridInner(Grid grid) {		
		final int nd = numDoses();
		cmpDoseCheckboxes = new CheckBox[chosenCompounds.size() * nd];
		doseTimeCheckboxes = new CheckBox[numDoses() * availableTimes.length];
		unitUis.clear();
		
		super.drawGridInner(grid);
	}	
	
	@Override
	protected void samplesAvailable() {
		for (BUnit u: availableUnits) {
			// Register these first so they can be looked up
			// later
			if (u.getDose().equals("Control")) {
				controlUnits.put(u.toString(), u);
			}	
		}
		
		for (BUnit u: availableUnits) {
//			Window.alert(u.toString());
			if (u.getDose().equals("Control")) {
				continue;
			}
			if (u.getSamples() == null || u.getSamples().length == 0) {				
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
			
			int cIdx = chosenCompounds.indexOf(u.getCompound());
			int dIdx = doseToIndex(u.getDose());
			int tIdx = SharedUtils.indexOf(availableTimes, u.getTime());
			
			if (cIdx == -1 || dIdx == -1 || tIdx == -1) {
				Window.alert("Data error");
				return;
			}
			
			cmpDoseCheckboxes[cIdx * numDoses() + dIdx].setEnabled(true);
			doseTimeCheckboxes[dIdx * availableTimes.length + tIdx]
					.setEnabled(true);

		}
		if (oldSelection != null) {
			setSelection(oldSelection);
			oldSelection = null;
		}	
	}
}
