package otgviewer.client;

import java.util.List;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.BUnit;
import otgviewer.shared.OTGSample;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;
import t.common.shared.sample.Annotation;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A time and dose grid that can show some variable as a heat map.
 * The variable is supplied as a microarray sample annotation.
 * @author johan
 *
 */
public class AnnotationTDGrid extends TimeDoseGrid {
	
	private HTML[][] labels;
	
	private ListBox annotationSelector;
	private Button annotationButton;
	
	public AnnotationTDGrid(Screen screen) {
		super(screen, false);
	}
	
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
	}

	@Override
	public void compoundsChanged(List<String> compounds) {
		super.compoundsChanged(compounds);
		
		if (annotationSelector.getItemCount() == 0 && compounds.size() > 0) {
			SampleClass sc = chosenSampleClass.copy();
			sc.put("compound_name", compounds.get(0));
			sparqlService.samples(sc, new AsyncCallback<OTGSample[]>() {
				public void onSuccess(OTGSample[] bcs) {
					
					sparqlService.annotations(bcs[0], new AsyncCallback<Annotation>() {
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
		
		SampleClass sc = chosenSampleClass.copy();
		sc.put("dose_level", dose);
		sc.put("exposure_time", time);
		sc.put("compound_name", compound);
		
		sparqlService.samples(sc,
				new PendingAsyncCallback<OTGSample[]>(this, "Unable to retrieve barcodes for the group definition.") {
					public void handleSuccess(OTGSample[] barcodes) {
						processAnnotationBarcodes(annotation, row, col, time, barcodes);						
					}
				});
	}
	
	private void processAnnotationBarcodes(final String annotation, final int row, final int col,
			final String time, final OTGSample[] barcodes) {
		final NumberFormat fmt = NumberFormat.getFormat("#0.00");
		Group g = new Group("temporary", barcodes, null);
		sparqlService.annotations(g, false, 
				new PendingAsyncCallback<Annotation[]>(this, "Unable to get annotations.") {
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
				
				double avg = (n > 0 ? sum / n : Double.NaN);
				labels[row][col].setText(time + " (" + fmt.format(avg) + ")");
				annotValues[row][col] = avg;
				annotValuesRemaining -= 1;
				
				if (annotValuesRemaining == 0) { 
					//got the final values
					double min = Double.MAX_VALUE;
					double max = Double.MIN_VALUE;
					
					for (double[] r : annotValues) {
						for (double v: r) {
							if (v != Double.NaN && v > max) {
								max = v;
							}
							if (v != Double.NaN && v < min) {
								min = v;
							}
						}
					}
					for (int r = 0; r < annotValues.length; ++r) {
						for (int c = 0; c < annotValues[0].length; ++c) {
							if (annotValues[r][c] != Double.NaN) {
								int gg = 255 - (int) ((annotValues[r][c] - min) * 127 / (max - min));
								int rr = gg;
								setColour(r, c, rr, gg, 255);
							}
						}
					}									
				}		
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
	protected Widget guiForUnit(BUnit unit) {
		int time = SharedUtils.indexOf(availableTimes, unit.getTime());
		int compound = chosenCompounds.indexOf(unit.getCompound());
		int dose = doseToIndex(unit.getDose());
		HTML r = new HTML(unit.getTime());
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
