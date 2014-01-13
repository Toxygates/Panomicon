package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.client.components.Screen;
import otgviewer.shared.BUnit;
import otgviewer.shared.Barcode;
import bioweb.shared.SharedUtils;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
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
	
	private Map<BUnit, CheckBox> unitCheckboxes = new HashMap<BUnit, CheckBox>();
	private Map<String, BUnit> controlUnits = new HashMap<String, BUnit>();
	
	public SelectionTDGrid(Screen screen) {
		super(screen, true);
	}
	
	@Override
	public void compoundsChanged(List<String> compounds) {
		oldSelection = getSelectedCombinations();		
		super.compoundsChanged(compounds);		
	}

	public void setAll(boolean val) {
		for (CheckBox cb : unitCheckboxes.values()) {
			cb.setValue(val);
		}
	}
	
	protected void setSelected(BUnit unit, boolean v) {
		CheckBox cb = unitCheckboxes.get(unit);
		if (cb != null) {
			cb.setValue(v);
		}
	}

	protected void setSelection(BUnit[] units) {
		for (BUnit u : units) {
			setSelected(u, true);
		}
	}
	
	protected boolean getSelected(BUnit unit) {
		return unitCheckboxes.get(unit).getValue();
	}

	protected BUnit[] getSelectedCombinations() {
		List<BUnit> r = new ArrayList<BUnit>();
		for (BUnit u: unitCheckboxes.keySet()) {
			CheckBox cb = unitCheckboxes.get(u);
			if (cb.getValue()) {
				r.add(u);
			}
		}
		return r.toArray(new BUnit[0]);		
	}
	
	public void setSelection(Barcode[] barcodes) {
		setAll(false);
		for (Barcode b: barcodes) {
			if (!b.getDose().equals("Control")) {
				setSelected(new BUnit(b), true);
			}
		}		
	}
	
	private abstract class UnitMultiSelector implements ValueChangeHandler<Boolean> {
		public void onValueChange(ValueChangeEvent<Boolean> vce) {
			for (BUnit b: unitCheckboxes.keySet()) {
				if (filter(b) && unitCheckboxes.get(b).isEnabled()) {
					unitCheckboxes.get(b).setValue(vce.getValue());					
				}
			}			
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
		for (BUnit k : unitCheckboxes.keySet()) {
			if (unitCheckboxes.get(k).getValue()) {
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
		return controlUnits.get(b.toString());
	}
	
	public List<BUnit> getSelectedUnits() {
		List<BUnit> r = new ArrayList<BUnit>();
		for (BUnit k : unitCheckboxes.keySet()) {
			if (unitCheckboxes.get(k).getValue()) {
				r.add(k);
				BUnit control = controlUnitFor(k);
				if (control != null) {
					r.add(control); 
				}
			}
		}
		return r;
	}
	
	@Override
	protected Widget guiForUnit(BUnit unit) {
		CheckBox cb = new CheckBox(unit.getTime());
		cb.setEnabled(false); //disabled by default until samples have been confirmed
		unitCheckboxes.put(unit, cb);
		cb.setValue(initState);							
		return cb;
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
		CheckBox cb = new CheckBox(availableTimes[time]);
		cb.setEnabled(false); //disabled by default until samples have been confirmed
		final int col = dose * availableTimes.length + time;
		doseTimeCheckboxes[col] = cb;
		cb.addValueChangeHandler(new DoseTimeSelectHandler(indexToDose(dose),
				availableTimes[time]));
		return cb;
	}

	private boolean initState = false;
	
	protected void drawGridInner(Grid grid, boolean initState) {
		this.initState = initState;		
		drawGridInner(grid);		
	}
	
	@Override
	protected void drawGridInner(Grid grid) {		
		final int nd = numDoses();
		cmpDoseCheckboxes = new CheckBox[chosenCompounds.size() * nd];
		doseTimeCheckboxes = new CheckBox[numDoses() * availableTimes.length];
		unitCheckboxes.clear();
		
		super.drawGridInner(grid);
		this.initState = false;
	}	
	
	@Override
	protected void samplesAvailable() {
		final int nd = numDoses();
		for (BUnit u: availableUnits) {
//			Window.alert(u.toString());
			if (u.getDose().equals("Control")) {
				controlUnits.put(u.toString(), u);
				continue;
			}
			if (u.getSamples() == null || u.getSamples().length == 0) {
				continue;
			}

			CheckBox cb = unitCheckboxes.get(u);
			unitCheckboxes.remove(u);
			// Remove the key and replace it since the ones from availableUnits
			// will be populated with concrete Barcodes (getSamples)
			unitCheckboxes.put(u, cb);
			cb.setEnabled(true);
			int cIdx = chosenCompounds.indexOf(u.getCompound());
			int dIdx = doseToIndex(u.getDose());
			int tIdx = SharedUtils.indexOf(availableTimes, u.getTime());
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
