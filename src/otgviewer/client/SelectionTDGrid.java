package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.Barcode;
import otgviewer.shared.SharedUtils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

public class SelectionTDGrid extends TimeDoseGrid {

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
	
	@Override
	protected void initTools(HorizontalPanel toolPanel) {
		super.initTools(toolPanel);
//		toolPanel.add(new Button("Select all", setAllHandler(true)));		
//		toolPanel.add(new Button("Select none", setAllHandler(false)));		
	}
	
	@Override
	public void compoundsChanged(List<String> compounds) {
		oldSelection = getSelection();		
		super.compoundsChanged(compounds);
	}

	private ClickHandler setAllHandler(final boolean val) {
		return new ClickHandler() {
		public void onClick(ClickEvent ce) {
			setAll(val);	
		}
		};
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
	
	private boolean getSelected(String compound, String time, String dose) {		
		int t = SharedUtils.indexOf(availableTimes, time);
		int d = doseToIndex(dose);
		return getSelected(compound, t, d);		
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
				for (int d = 0; d < 3; d++) {
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
	
	private class MultiSelectHandler implements ValueChangeHandler<Boolean> {
		private int from, to, row;
		MultiSelectHandler(int row, int from, int to) {
			this.from = from;
			this.to = to;
			this.row = row;
		}
		
		public void onValueChange(ValueChangeEvent<Boolean> vce) {
			for (int i = from; i < to; ++i) {
				checkboxes[row][i].setValue(vce.getValue());
			}
		}
	}
	
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
						final String compound = chosenCompounds.get(c);
						final String dose = indexToDose(d);						
						final String time = availableTimes[t];						
						outstanding += 1;
						gotSome = true;
						owlimService.barcodes(chosenDataFilter, compound,
								dose, time,
								new AsyncCallback<Barcode[]>() {
									public void onSuccess(Barcode[] barcodes) {
										if (barcodes.length == 0) {
											Window.alert("No samples found for " + compound + "/" + dose + "/" + time);
										} else {
											for (Barcode b: barcodes) {
												obtainedBarcodes.add(b);
											}							
										}
										decrementOutstanding();
									}

									public void onFailure(Throwable caught) {
										Window.alert("Unable to retrieve sample information.");
										decrementOutstanding();
									}
								});
					}
				}
			}
		}	
		if (!gotSome) {
			Window.alert("Please select at least one time/dose combination.");		
			outstandingListener.barcodesObtained(obtainedBarcodes); //ensure that we always call back at least once
		}
	}
	
	private synchronized void decrementOutstanding() {
		outstanding -= 1;
		if (outstanding == 0) {
			outstandingListener.barcodesObtained(obtainedBarcodes);
		}
	}

	
	@Override
	protected Widget initUnit(int compound, int dose, int time) {
		CheckBox cb = new CheckBox(availableTimes[time]);
		cb.setValue(initState);					
		checkboxes[compound][availableTimes.length * dose + time] = cb;
		return cb;
	}


	@Override
	protected Widget finaliseGroup(int compound, int dose) {
		CheckBox all = new CheckBox("All");
		all.addValueChangeHandler(new MultiSelectHandler(compound,
				availableTimes.length * dose, availableTimes.length * (dose + 1)));
		return all;		
	}

	private boolean initState = false;
	
	protected void drawGridInner(Grid grid, boolean initState) {
		this.initState = initState;		
		super.drawGridInner(grid);		
	}
	
	@Override
	protected void drawGridInner(Grid grid) {		
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
	}
}
