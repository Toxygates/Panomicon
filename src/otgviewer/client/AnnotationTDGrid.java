package otgviewer.client;

import java.util.List;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.shared.Annotation;
import otgviewer.shared.Barcode;
import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class AnnotationTDGrid extends TimeDoseGrid {
	
	private HTML[][] labels;
	
	private ListBox annotationSelector;
	private Button annotationButton;
	
	@Override
	protected void initTools(HorizontalPanel toolPanel) {
		super.initTools(toolPanel);
		toolPanel.add(new Label("Annotation:"));
		annotationSelector = new ListBox();
		toolPanel.add(annotationSelector);
		annotationButton = new Button("Show");
		annotationButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ce) {
				reloadAnnotations();
			}
		});
		toolPanel.add(annotationButton);		
	}

	@Override
	public void dataFilterChanged(DataFilter filter) {
		super.dataFilterChanged(filter);
		boolean annEnab = (filter.cellType == CellType.Vitro ? false : true);
		annotationSelector.setEnabled(annEnab);
		annotationButton.setEnabled(annEnab);
	}

	@Override
	public void compoundsChanged(List<String> compounds) {
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
	}
	
	private void setColour(int r, int c, int rr, int gg, int bb) {
		String html = labels[r][c].getHTML();
		labels[r][c].setHTML("<div style=\"background: #" + Integer.toHexString(rr) + 
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
				labels[row][col].setText(time + " (" + fmt.format(avg) + ")");
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
	
	@Override
	protected Widget initUnit(int compound, int dose, int time) {		
		HTML r = new HTML(availableTimes[time]);
		r.setStyleName("slightlySpaced");
		labels[compound][availableTimes.length * dose + time] = r;
		return r;
	}

	@Override
	protected void drawGridInner(Grid grid) {
		labels = new HTML[chosenCompounds.size()][];
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			labels[c] = new HTML[3 * availableTimes.length];
		}
		super.drawGridInner(grid);
	}

}
