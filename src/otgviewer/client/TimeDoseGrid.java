package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.shared.Annotation;
import otgviewer.shared.Barcode;
import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.SharedUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A widget that displays times and doses for a number of compounds in a grid
 * layout. Each time and dose combination can be selected individually.
 * @author johan
 *
 */
public class TimeDoseGrid extends DataListenerWidget {
	private Grid grid = new Grid();
	private String[] availableTimes;
	
	private CheckBox[][] checkboxes; //for selecting the subgroups
	private ListBox annotationSelector = new ListBox();
	//private List<String> oldCompounds = new ArrayList<String>();
	private Combination[] oldSelection;
	
	private Button annotationButton;
	
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);

	public static interface BarcodeListener {
		void barcodesObtained(Barcode[] barcodes, String description);
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
	
	public TimeDoseGrid() {
		VerticalPanel vp = new VerticalPanel();
		initWidget(vp);
		
		HorizontalPanel horizontalPanel_1 = Utils.mkHorizontalPanel();		
		vp.add(horizontalPanel_1);
		
		Button btnSelectAll = new Button("Select all");
		horizontalPanel_1.add(btnSelectAll);
		btnSelectAll.addClickHandler(setAllHandler(true));
		
		Button btnSelectNone = new Button("Select none");
		horizontalPanel_1.add(btnSelectNone);
		btnSelectNone.addClickHandler(setAllHandler(false));			
		
		horizontalPanel_1.add(new Label("Annotation:"));
		horizontalPanel_1.add(annotationSelector);
		annotationButton = new Button("Show");
		annotationButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ce) {
				reloadAnnotations();
			}
		});
		horizontalPanel_1.add(annotationButton);
		horizontalPanel_1.setSpacing(2);
		
		grid.setStyleName("highlySpaced");
		grid.setWidth("700px");
		grid.setHeight("400px");
		grid.setBorderWidth(0);
		vp.add(grid);
	}
	
	private ClickHandler setAllHandler(final boolean val) {
		return new ClickHandler() {
		public void onClick(ClickEvent ce) {
			for (CheckBox[] r: checkboxes) {
				for (CheckBox cb: r) {
					cb.setValue(val);
				}
			}				
		}
		};
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {
		if (!filter.equals(chosenDataFilter)) {
			super.dataFilterChanged(filter);
			availableTimes = null;
			fetchTimes();
		} else {
			super.dataFilterChanged(filter);
		}
		
		boolean annEnab = (filter.cellType == CellType.Vitro ? false : true);
		annotationSelector.setEnabled(annEnab);
		annotationButton.setEnabled(annEnab);
	}
	
	
	@Override
	public void compoundsChanged(List<String> compounds) {		
		oldSelection = getSelection();
		
		super.compoundsChanged(compounds);		
		if (annotationSelector.getItemCount() == 0 && compounds.size() > 0) {
			owlimService.barcodes(chosenDataFilter, compounds.get(0), null, null, new AsyncCallback<Barcode[]>() {
				public void onSuccess(Barcode[] bcs) {
					
					owlimService.annotations(bcs[0], new AsyncCallback<Annotation>() {
						public void onSuccess(Annotation a) {
							for (Annotation.Entry e: a.getEntries()) {
								if (e.numerical) {
									annotationSelector.addItem(e.description);
								}
							}
						}
						public void onFailure(Throwable caught) {
							Window.alert("Unable to get annotations.");
						}
					});
					
				}
				public void onFailure(Throwable caught) {
					Window.alert("Unable to get annotations.");
				}
			});
		}
		redrawGrid();
	}
	
	private void lazyFetchTimes() {
		if (availableTimes != null && availableTimes.length > 0) {
			drawGridInner(false);
		} else {
			fetchTimes();						
		}		
	}
	
	private void fetchTimes() {		
		owlimService.times(chosenDataFilter, null, new AsyncCallback<String[]>() {
			public void onSuccess(String[] times) {
				availableTimes = times;
				drawGridInner(false);
				//TODO: block compound selection until we have obtained this data
			}
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get sample times.");
			}
		});			
	}

	private int doseToIndex(String dose) {
		if (dose.equals("Low")) {
			return 0;
		} else if (dose.equals("Middle")) {
			return 1;
		} else {
			return 2;
		}
	}
	
	private String indexToDose(int dose) {
		switch (dose) {
		case 0:
			return "Low";			
		case 1:
			return "Middle";								
		}
		return "High";
	}
	
	private void redrawGrid() {
		grid.resize(chosenCompounds.size() + 1, 4);
		
		for (int i = 1; i < chosenCompounds.size() + 1; ++i) {
			
			grid.setWidget(i, 0, Utils.mkEmphLabel(chosenCompounds.get(i - 1)));
		}
				
		grid.setWidget(0, 1, Utils.mkEmphLabel("Low"));		
		grid.setWidget(0, 2, Utils.mkEmphLabel("Medium"));		
		grid.setWidget(0, 3, Utils.mkEmphLabel("High"));
		
		grid.setHeight(50 * (chosenCompounds.size() + 1) + "px");
		lazyFetchTimes();		
	}
	
	private void drawGridInner(boolean initState) {
		checkboxes = new CheckBox[chosenCompounds.size()][];
		
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			checkboxes[c] = new CheckBox[3 * availableTimes.length];
			for (int d = 0; d < 3; ++d) {
				HorizontalPanel hp = new HorizontalPanel();
				for (int t = 0; t < availableTimes.length; ++t) {				
					CheckBox cb = new CheckBox(availableTimes[t]);
					cb.setValue(initState);					
					checkboxes[c][availableTimes.length * d + t] = cb;
					hp.add(cb);					
					
				}
				CheckBox all = new CheckBox("All");
				all.addValueChangeHandler(new MultiSelectHandler(c, availableTimes.length * d, availableTimes.length * (d + 1)));
				hp.add(all);
				
				SimplePanel sp = new SimplePanel(hp);
				sp.setStyleName("border");					
				grid.setWidget(c + 1, d + 1, sp);					
			}
		}
		if (oldSelection != null) {
			setSelection(oldSelection);
			oldSelection = null;
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
		for (String c: chosenCompounds) {
			for (int d = 0; d < 3; d++) {
				for (int t = 0; t < availableTimes.length; ++t) {
					if (getSelected(c, t, d)) {
						r.add(new Combination(c, d, t));
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
	
	public void getSelection(final BarcodeListener listener) {
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			for (int d = 0; d < 3; ++d) {
				for (int t = 0; t < availableTimes.length; ++t) {
					if (checkboxes[c][availableTimes.length * d + t].getValue()) {
						final String compound = chosenCompounds.get(c);
						final String dose = indexToDose(d);						
						final String time = availableTimes[t];
						
						owlimService.barcodes(chosenDataFilter, compound,
								dose, time,
								new AsyncCallback<Barcode[]>() {
									public void onSuccess(Barcode[] barcodes) {
										listener.barcodesObtained(barcodes, compound + "/" + dose + "/" + time);										
									}

									public void onFailure(Throwable caught) {
										Window.alert("Unable to retrieve sample information.");
									}
								});
					}
				}
			}
		}		
	}
	
	//ANNOTATION DISPLAY FUNCTIONS
	
	private void reloadAnnotations() {
		if (annotationSelector.getSelectedIndex() != -1) {
			String annot = annotationSelector.getItemText(annotationSelector.getSelectedIndex());
			displayAnnotation(annot);			
		}
	}
	
	private void displayAnnotation(String name) {
		annotValues = new double[chosenCompounds.size()][availableTimes.length * 3];
		annotValuesRemaining = chosenCompounds.size() * availableTimes.length * 3;
		
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			for (int d = 0; d < 3; ++d) {
				for (int t = 0; t < availableTimes.length; ++t) {
					final String compound = chosenCompounds.get(c);
					final String dose = indexToDose(d);

					final String time = availableTimes[t];
					displayAnnotation(name, c, d * availableTimes.length + t,
							compound, dose, time);
				}
			}
		}		
	}
	
	private void setColour(int r, int c, int rr, int gg, int bb) {
		String html = checkboxes[r][c].getHTML();
		checkboxes[r][c].setHTML("<div style=\"background: #" + Integer.toHexString(rr) + 
				Integer.toHexString(gg) + Integer.toHexString(bb) + "\">" + html + "</div>"); 			
	}
	
	private double[][] annotValues;
	private int annotValuesRemaining = 0;
	
	private void displayAnnotation(final String annotation, final int row, final int col, 
			final String compound, final String dose, final String time) {		
		
		owlimService.barcodes(chosenDataFilter, compound,
				dose, time,
				new PendingAsyncCallback<Barcode[]>(this) {
					public void handleSuccess(Barcode[] barcodes) {
						processAnnotationBarcodes(annotation, row, col, time, barcodes);						
					}

					public void handleFailure(Throwable caught) {
						Window.alert("Unable to retrieve barcodes for the group definition.");						
					}
				});
	}
	
	private void processAnnotationBarcodes(final String annotation, final int row, final int col,
			final String time, final Barcode[] barcodes) {
		final NumberFormat fmt = NumberFormat.getFormat("#0.00");
		Group g = new Group("temporary", barcodes);
		owlimService.annotations(g, new PendingAsyncCallback<Annotation[]>(this) {
			public void handleSuccess(Annotation[] as) {								
				double sum = 0;
				int n = 0;
				for (Annotation a: as) {
					try {
						sum += a.doubleValueFor(annotation);
						n += 1;
					} catch (Exception e) {
						//number format error, or missing
					}									
				}
				
				double avg = (n > 0 ? sum / n : 0);
				checkboxes[row][col].setText(time + " (" + fmt.format(avg) + ")");
				annotValues[row][col] = avg;
				annotValuesRemaining -= 1;
				
				if (annotValuesRemaining == 0) { 
					//got the final values
					double min = Double.MAX_VALUE;
					double max = Double.MIN_VALUE;
					
					for (double[] r : annotValues) {
						for (double v: r) {
							if (v > max) {
								max = v;
							}
							if (v < min) {
								min = v;
							}
						}
					}
					for (int r = 0; r < annotValues.length; ++r) {
						for (int c = 0; c < annotValues[0].length; ++c) {
							int bb = 255;
							int gg = 255 - (int) ((annotValues[r][c] - min) * 127 / (max - min));
							int rr = gg;
							setColour(r, c, rr, gg, 255);
						}
					}									
				}		
			}
			public void handleFailure(Throwable caught) {
				Window.alert("Unable to get annotations.");				
			}
		});
	}
	
	
}
