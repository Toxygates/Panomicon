package otgviewer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import otgviewer.client.components.Screen;
import otgviewer.shared.Barcode;
import bioweb.shared.SharedUtils;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
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
	private CheckBox[][] checkboxes; //for selecting the subgroups	
	private Combination[] oldSelection;

	private static class Combination {
		String compound;
		int dose;
		int time;
		
		public Combination(String compound, int dose, int time) {
			this.compound = compound;
			this.dose = dose;
			this.time = time;
		}
	}
	
	public SelectionTDGrid(Screen screen) {
		super(screen, true);
	}
	
	@Override
	protected void initTools(HorizontalPanel toolPanel) {
		super.initTools(toolPanel);
	}
	
	@Override
	public void compoundsChanged(List<String> compounds) {
		oldSelection = getSelectedCombinations();		
		super.compoundsChanged(compounds);		
	}

	public void setAll(boolean val) {
		if (checkboxes != null) {
			for (CheckBox[] r : checkboxes) {
				for (CheckBox cb : r) {
					cb.setValue(val);
				}
			}
		}
	}
	
	private void setSelected(String compound, String time, String dose, boolean v) {
		int t = SharedUtils.indexOf(availableTimes, time);
		int d = doseToIndex(dose);
		setSelected(compound, t, d, v);
	}
	
	private boolean getSelected(String compound, int t, int d) {
		int ci = SharedUtils.indexOf(chosenCompounds, compound);					
		return checkboxes[ci][d * availableTimes.length + t].getValue();
	}
	
	private void setSelected(String compound, int t, int d, boolean v) {
		int ci = SharedUtils.indexOf(chosenCompounds, compound);
		if (ci != -1) {
			checkboxes[ci][d * availableTimes.length + t].setValue(v);
		}
	}
	
	protected Combination[] getSelectedCombinations() {
		List<Combination> r = new ArrayList<Combination>();
		if (availableTimes != null) {
			for (String c : chosenCompounds) {
				for (int d = 0; d < numDoses(); d++) {
					for (int t = 0; t < availableTimes.length; ++t) {
						if (getSelected(c, t, d)) {
							r.add(new Combination(c, d, t));
						}
					}
				}
			}
		}
		return r.toArray(new Combination[0]);
	}
	
	public void setSelection(Barcode[] barcodes) {
		setAll(false);
		for (Barcode b: barcodes) {
			String c = b.getCompound();
			setSelected(c, b.getTime(), b.getDose(), true);						
		}		
	}
	
	protected void setSelection(Combination[] combinations) {
		for (Combination c: combinations) {
			setSelected(c.compound, c.time, c.dose, true);
		}
	}
	
	private class RowMultiSelectHandler implements ValueChangeHandler<Boolean> {
		private int from, to, row;
		RowMultiSelectHandler(int row, int from, int to) {
			this.from = from;
			this.to = to;
			this.row = row;
		}
		
		public void onValueChange(ValueChangeEvent<Boolean> vce) {
			for (int i = from; i < to; ++i) {
				if (checkboxes[row][i].isEnabled()) {
					checkboxes[row][i].setValue(vce.getValue());
				}
			}
		}
	}
	
	private class ColumnMultiSelectHandler implements ValueChangeHandler<Boolean> {
		private int col;
		ColumnMultiSelectHandler(int col) {
			this.col = col;
		}
		
		public void onValueChange(ValueChangeEvent<Boolean> vce) {
			for (int i = 0; i < checkboxes.length; ++i) {
				if (checkboxes[i][col].isEnabled()) {
					checkboxes[i][col].setValue(vce.getValue());
				}
			}
		}
	}
	
	public List<Barcode> getSelectedBarcodes() {
		final int nd = numDoses();
		List<Barcode> r = new ArrayList<Barcode>();
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			for (int d = 0; d < nd; ++d) {
				for (int t = 0; t < availableTimes.length; ++t) {
					if (checkboxes[c][availableTimes.length * d + t].getValue()) {
						String compound = chosenCompounds.get(c);
						String key = compound + ":" + indexToDose(d) + ":" + availableTimes[t];
						if (availableSamples.containsKey(key)) {							
							r.addAll(availableSamples.get(key));	
						}						
					}
				}
			}
		}	
		if (r.isEmpty()) {
			Window.alert("Please select at least one time/dose combination.");
		}		
		return r;
	}
	
	@Override
	protected Widget guiFor(int compound, int dose, int time) {
		CheckBox cb = new CheckBox(availableTimes[time]);
		cb.setEnabled(false); //disabled by default until samples have been confirmed
		cb.setValue(initState);					
		checkboxes[compound][availableTimes.length * dose + time] = cb;
		return cb;
	}

	@Override
	protected Widget guiForCompoundDose(int compound, int dose) {
		final int nd = numDoses();
		CheckBox all = new CheckBox("All");
		all.setEnabled(false); //disabled by default until samples have been confirmed
		cmpDoseCheckboxes[compound * nd + dose] = all;
		all.addValueChangeHandler(new RowMultiSelectHandler(compound,
				availableTimes.length * dose, availableTimes.length * (dose + 1)));
		return all;		
	}

	@Override
	protected Widget guiForDoseTime(int dose, int time) {
		CheckBox cb = new CheckBox(availableTimes[time]);
		cb.setEnabled(false); //disabled by default until samples have been confirmed
		final int col = dose * availableTimes.length + time;
		doseTimeCheckboxes[col] = cb;
		cb.addValueChangeHandler(new ColumnMultiSelectHandler(col));
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
		checkboxes = new CheckBox[chosenCompounds.size()][];
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			checkboxes[c] = new CheckBox[nd * availableTimes.length];			
		}
		super.drawGridInner(grid);
		this.initState = false;
		if (oldSelection != null) {
			setSelection(oldSelection);
			oldSelection = null;
		}
	}
	
	@Override
	protected void samplesAvailable() {
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			getTimeDoseCombinations(chosenCompounds.get(c), c);
		}
	}

	/**
	 * Obtain time/dose combinations for a given compound that actually have samples in the database.
	 * Only enable those checkboxes that have such corresponding samples.
	 * @param compound
	 * @param compoundRow
	 */
	private void getTimeDoseCombinations(String compound, final int compoundRow) {
		final int nd = numDoses();
		for (int d = 0; d < nd; ++d) {
			for (int t = 0; t < availableTimes.length; ++t) {
				String k = compound + ":" + indexToDose(d) + ":" + availableTimes[t];
				if (!availableSamples.containsKey(k)) {
					continue;
				}
				List<Barcode> bcs = availableSamples.get(k);
				if (bcs.size() > 0) {
					checkboxes[compoundRow][availableTimes.length * d + t].setEnabled(true);
					cmpDoseCheckboxes[compoundRow * nd + d].setEnabled(true);
					doseTimeCheckboxes[d * availableTimes.length + t].setEnabled(true);
				}
			}
		}
	}
}
