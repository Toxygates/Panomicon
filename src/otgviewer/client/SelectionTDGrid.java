package otgviewer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.Barcode;
import bioweb.shared.Pair;
import bioweb.shared.SharedUtils;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A time/dose grid for defining and editing sample groups in terms of time/dose
 * combinations for particular compounds.
 * 
 * TODO: There is too much network communication here. (First get the time/dose 
 * combinations, then enable checkboxes, then eventually get samples.)
 * Better would be to simply get all samples and their attributes immediately when a
 * compound has been selected.
 * @author johan
 *
 */
public class SelectionTDGrid extends TimeDoseGrid {

	private CheckBox[] cmpDoseCheckboxes; //selecting all samples for a cmp/dose combo
	private CheckBox[] doseTimeCheckboxes; //selecting all samples for a dose/time combo
	private CheckBox[][] checkboxes; //for selecting the subgroups	
	private Combination[] oldSelection;
	
	public static interface BarcodeListener {
		void barcodesObtained(List<Barcode> barcodes);
	}

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
		oldSelection = getSelection();		
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
	
	public Combination[] getSelection() {
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
	
	public void setSelection(Combination[] combinations) {
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
	
	/**
	 * How many outstanding RPC calls are we waiting for?
	 */
	private int outstanding = 0;
	private List<Barcode> obtainedBarcodes;
	private BarcodeListener outstandingListener;
	
	public synchronized void getSelection(final BarcodeListener listener) {
		boolean gotSome = false;
		outstanding = 0;
		outstandingListener = listener;
		obtainedBarcodes = new ArrayList<Barcode>();
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			for (int d = 0; d < 3; ++d) {
				for (int t = 0; t < availableTimes.length; ++t) {
					if (checkboxes[c][availableTimes.length * d + t].getValue()) {						
						outstanding += 1;
						gotSome = true;
						getBarcodes(chosenCompounds.get(c), indexToDose(d), availableTimes[t], obtainedBarcodes);
					}
				}
			}
		}	
		if (!gotSome) {
			Window.alert("Please select at least one time/dose combination.");		
			outstandingListener.barcodesObtained(obtainedBarcodes); //ensure that we always call back at least once
		}
	}
	
	private void getBarcodes(final String compound, final String dose, final String time, final List<Barcode> addTo) {
		sparqlService.barcodes(chosenDataFilter, compound, dose, time,
			new PendingAsyncCallback<Barcode[]>(this) {
				public void handleSuccess(Barcode[] barcodes) {
					if (barcodes.length == 0) {
						Window.alert("No samples found for " + compound
								+ "/" + dose + "/" + time);
					} else {
						for (Barcode b : barcodes) {
							obtainedBarcodes.add(b);
						}
					}
					decrementOutstanding();
				}

				public void handleFailure(Throwable caught) {
					Window.alert("Unable to retrieve sample information.");
					decrementOutstanding();
				}
			});
	}
	
	/**
	 * Reduce the number of outstanding RPC responses we are waiting for.
	 */
	private synchronized void decrementOutstanding() {
		outstanding -= 1;
		if (outstanding == 0) {
			outstandingListener.barcodesObtained(obtainedBarcodes);
		}
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
		CheckBox all = new CheckBox("All");
		all.setEnabled(false); //disabled by default until samples have been confirmed
		cmpDoseCheckboxes[compound * 3 + dose] = all;
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
		cmpDoseCheckboxes = new CheckBox[chosenCompounds.size() * 3];
		doseTimeCheckboxes = new CheckBox[numDoses() * availableTimes.length];
		checkboxes = new CheckBox[chosenCompounds.size()][];
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			checkboxes[c] = new CheckBox[3 * availableTimes.length];			
		}
		super.drawGridInner(grid);
		this.initState = false;
		if (oldSelection != null) {
			setSelection(oldSelection);
			oldSelection = null;
		}				
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
		sparqlService.timeDoseCombinations(chosenDataFilter, compound, new AsyncCallback<Pair<String,String>[]>() {			
			@Override
			public void onSuccess(Pair<String, String>[] result) {
				boolean gotNonControl = false;
				for (Pair<String, String> timeDose: result) {
					String time = timeDose.first();
					String dose = timeDose.second();					
					int di = doseToIndex(dose);
					int ti = SharedUtils.indexOf(availableTimes, time);
					if (di != -1 && ti != -1) {
						gotNonControl = true;
						checkboxes[compoundRow][availableTimes.length * di + ti].setEnabled(true);
						cmpDoseCheckboxes[compoundRow * 3 + di].setEnabled(true);
						doseTimeCheckboxes[di * availableTimes.length + ti].setEnabled(true);
					}					
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Error: Unable to obtain time/dose combinations.");
			}
		});
	}
}
