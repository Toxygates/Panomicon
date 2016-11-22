/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package otgviewer.client;

import java.util.List;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.BioParamValue;
import otgviewer.shared.NumericalBioParamValue;
import t.common.shared.SampleClass;
import t.common.shared.sample.Annotation;
import t.common.shared.sample.Group;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A time and dose grid that can show some variable as a heat map. The variable is supplied as a
 * microarray sample annotation.
 * 
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
  public void compoundsChanged(List<String> compounds) {
    super.compoundsChanged(compounds);

    if (annotationSelector.getItemCount() == 0 && compounds.size() > 0) {
      SampleClass sc = chosenSampleClass.copy();
      sc.put("compound_name", compounds.get(0));
      sparqlService.samples(sc, new PendingAsyncCallback<Sample[]>(
          this, "Unable to get samples") {
        public void handleSuccess(Sample[] bcs) {

          sparqlService.annotations(bcs[0], new PendingAsyncCallback<Annotation>(
              AnnotationTDGrid.this, "Unable to get annotations.") {
            public void handleSuccess(Annotation a) {
              for (BioParamValue e : a.getAnnotations()) {
                if (e instanceof NumericalBioParamValue) {
                  annotationSelector.addItem(e.label());
                }
              }
            }
          });
        }
      });
    }
  }

  private void setColour(int r, int c, int rr, int gg, int bb) {
    String html = labels[r][c].getHTML();
    labels[r][c].setHTML("<div style=\"background: #" + Integer.toHexString(rr)
        + Integer.toHexString(gg) + Integer.toHexString(bb) + "\">" + html + "</div>");
  }

  private double[][] annotValues;
  private int annotValuesRemaining = 0;

  private void displayAnnotation(final String annotation, final int row, final int col,
      final String compound, final String dose, final String time) {

    SampleClass sc = chosenSampleClass.copy();
    sc.put("dose_level", dose);
    sc.put("exposure_time", time);
    sc.put("compound_name", compound);

    sparqlService.samples(sc, new PendingAsyncCallback<Sample[]>(this,
        "Unable to retrieve barcodes for the group definition.") {
      public void handleSuccess(Sample[] barcodes) {
        processAnnotationBarcodes(annotation, row, col, time, barcodes);
      }
    });
  }

  private void processAnnotationBarcodes(final String annotation, final int row, final int col,
      final String time, final Sample[] barcodes) {
    final NumberFormat fmt = NumberFormat.getFormat("#0.00");
    Group g = new Group(schema, "temporary", barcodes, null);
    sparqlService.annotations(g, false, new PendingAsyncCallback<Annotation[]>(this,
        "Unable to get annotations.") {
      public void handleSuccess(Annotation[] as) {
        double sum = 0;
        int n = 0;
        for (Annotation a : as) {
          try {
            double val = a.doubleValueFor(annotation);
            if (!Double.isNaN(val)) {
              n += 1;
              sum += val;
            }
          } catch (Exception e) {
            logger.warning("Number format error or unavailable data for " + annotation);
          }
        }

        double avg = (n > 0 ? sum / n : Double.NaN);
        labels[row][col].setText(time + " (" + fmt.format(avg) + ")");
        annotValues[row][col] = avg;
        annotValuesRemaining -= 1;

        if (annotValuesRemaining == 0) {
          // got the final values
          double min = Double.MAX_VALUE;
          double max = Double.MIN_VALUE;

          for (double[] r : annotValues) {
            for (double v : r) {
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
    int numMin = minorValues.size();
    annotValues = new double[chosenCompounds.size()][numMin * 3];
    annotValuesRemaining = chosenCompounds.size() * numMin * 3;

    for (int c = 0; c < chosenCompounds.size(); ++c) {
      for (int d = 0; d < 3; ++d) {
        for (int t = 0; t < numMin; ++t) {
          final String compound = chosenCompounds.get(c);
          final String dose = mediumValues.get(d);

          final String time = minorValues.get(t);
          displayAnnotation(name, c, d * numMin + t, compound, dose, time);
        }
      }
    }
  }

  @Override
  protected Widget guiForUnit(Unit unit) {
    int time = minorValues.indexOf(unit.get(timeParameter));
    int compound = chosenCompounds.indexOf(unit.get(majorParameter));
    int dose = mediumValues.indexOf(unit.get(mediumParameter));
    HTML r = new HTML(unit.get(timeParameter));
    r.setStylePrimaryName("slightlySpaced");
    labels[compound][minorValues.size() * dose + time] = r;
    return r;
  }

  @Override
  protected void drawGridInner(Grid grid) {
    labels = new HTML[chosenCompounds.size()][];
    for (int c = 0; c < chosenCompounds.size(); ++c) {
      labels[c] = new HTML[3 * minorValues.size()];
    }
    super.drawGridInner(grid);
  }

}
