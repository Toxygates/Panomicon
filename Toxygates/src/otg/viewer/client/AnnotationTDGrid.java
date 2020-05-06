/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package otg.viewer.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.*;
import otg.model.sample.OTGAttribute;
import otg.viewer.client.components.OTGScreen;
import otg.viewer.client.components.TimeDoseGrid;
import t.common.shared.Pair;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.viewer.client.Analytics;
import t.viewer.client.components.PendingAsyncCallback;
import t.viewer.client.future.Future;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * A time and dose grid that can show some variable as a mini heat map. The variable is supplied as a
 * microarray sample annotation.
 */
public class AnnotationTDGrid extends TimeDoseGrid {

  private Delegate delegate;
  
  private HTML[][] labels;

  private ListBox annotationSelector;
  private Button annotationButton;

  private Attribute[] currentAttributes;
  private Map<String, Sample[]> samplesForCompounds;
  
  public interface Delegate {
    void finishedFetchingAnnotations();
  }

  public AnnotationTDGrid(OTGScreen screen, Delegate delegate) {
    super(screen, false);
    this.delegate = delegate;
  }

  @Override
  protected void initTools(HorizontalPanel toolPanel) {
    super.initTools(toolPanel);
    toolPanel.add(new Label("Annotation:"));
    annotationSelector = new ListBox();
    toolPanel.add(annotationSelector);
    annotationButton = new Button("Show");
    annotationButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent ce) {
        reloadAnnotations();
        Analytics.trackEvent(Analytics.CATEGORY_VISUALIZATION,
            Analytics.ACTION_DISPLAY_MINI_HEATMAP);
      }
    });
    toolPanel.add(annotationButton);
  }

  @Override
  public Future<Pair<String[], Pair<Unit, Unit>[]>> initializeState(
      SampleClass sampleClass, List<String> compounds, boolean datasetsChanged) {
    
    Future<Pair<String[], Pair<Unit, Unit>[]>> future = 
        super.initializeState(sampleClass, compounds, datasetsChanged);
      
    // Clear labels from previous groups
    drawGridInner(grid);

    /* Get list of attributes relevant for the samples we want to look at.
       This code assumes that we can just look at the samples for the first
       compound in our compound list; this is probably true for Open TG-GATEs
       but could be a problem with other data.
    */
    if (annotationSelector.getItemCount() == 0 && compounds.size() > 0) {
      SampleClass sc = chosenSampleClass.copy();
      sc.put(OTGAttribute.Compound, compounds.get(0));
      sampleService.attributesForSamples(sc, new PendingAsyncCallback<Attribute[]>(
        screen, "Unable to get samples") {
          @Override
          public void handleSuccess(Attribute[] attributes) {
            currentAttributes = Arrays.stream(attributes).filter(a -> a.isNumerical()).toArray(Attribute[]::new);
            for (Attribute attribute: currentAttributes) {
              annotationSelector.addItem(attribute.title());
            }
          }
        }
      );
    }

    // Fetch samples (for each compound)
    samplesForCompounds = new HashMap<String, Sample[]>();
    for (String compound: compounds) {
      SampleClass sc = chosenSampleClass.copy();
      sc.put(OTGAttribute.Compound, compound);
      sampleService.samples(sc, new PendingAsyncCallback<Sample[]>(screen,
              "Unable to retrieve samples for the group definition.") {
        @Override
        public void handleSuccess(Sample[] samples) {
          samplesForCompounds.put(compound, samples);
        }
      });
    }

    return future;
  }

  private void setColour(int r, int c, int rr, int gg, int bb) {
    String html = labels[r][c].getHTML();
    labels[r][c].setHTML("<div style=\"background: #" + Integer.toHexString(rr)
        + Integer.toHexString(gg) + Integer.toHexString(bb) + "\">" + html + "</div>");
  }

  private double[][] parameterValues;
  private int valuesRemaining = 0;

  private void fetchValuesForAttribute(final Attribute attribute, final int row, final int col,
                                       final String compound, final String dose, final String time) {

    SampleClass sc = chosenSampleClass.copy();
    sc.put(OTGAttribute.DoseLevel, dose);
    sc.put(OTGAttribute.ExposureTime, time);
    sc.put(OTGAttribute.Compound, compound);

    Sample[] allSamplesForCompound = samplesForCompounds.get(compound);
    Sample[] cellSamples = Arrays.stream(allSamplesForCompound).
            filter(s -> s.sampleClass().compatible(sc)).toArray(Sample[]::new);
    getValuesForParameter(attribute, row, col, time, cellSamples);
  }

  private double doubleValueFor(Sample sample, Attribute parameter)
          throws IllegalArgumentException {
    String valueString = sample.get(parameter);
    if (valueString == null) {
      throw new IllegalArgumentException("Value not available");
    } else {
      return Double.parseDouble(valueString);
    }
  }
  
  private void getValuesForParameter(final Attribute parameter, final int row, final int col,
                                     final String time, final Sample[] samples) {
    final NumberFormat fmt = NumberFormat.getFormat("#0.00");
    sampleService.samplesWithAttributeValues(samples, false, new PendingAsyncCallback<Sample[]>(screen,
        "Unable to get annotations.") {
      @Override
      public void handleSuccess(Sample[] fetchedSamples) {
        double sum = 0;
        int n = 0;
        for (Sample sample : fetchedSamples) {
          try {
            double val = doubleValueFor(sample, parameter);
            if (!Double.isNaN(val)) {
              n += 1;
              sum += val;
            }
          } catch (IllegalArgumentException e) {
            logger.info("No value for parameter " + parameter.title() + " for sample " + sample.id());
          } catch (Exception e) {
            logger.log(Level.WARNING, "Annotation sample processing error", e);
          }
        }

        double avg = (n > 0 ? sum / n : Double.NaN);
        labels[row][col].setText(time + " (" + fmt.format(avg) + ")");
        parameterValues[row][col] = avg;
        valuesRemaining -= 1;

        if (valuesRemaining == 0) {
          // got the final values
          double min = Double.MAX_VALUE;
          double max = Double.MIN_VALUE;

          for (double[] r : parameterValues) {
            for (double v : r) {
              if (v != Double.NaN && v > max) {
                max = v;
              }
              if (v != Double.NaN && v < min) {
                min = v;
              }
            }
          }
          for (int r = 0; r < parameterValues.length; ++r) {
            for (int c = 0; c < parameterValues[0].length; ++c) {
              if (parameterValues[r][c] != Double.NaN) {
                int gg = 255 - (int) ((parameterValues[r][c] - min) * 127 / (max - min));
                int rr = gg;
                setColour(r, c, rr, gg, 255);
              }
            }
          }
        }
        delegate.finishedFetchingAnnotations();
      }
    });
  }

  private void reloadAnnotations() {
    if (annotationSelector.getSelectedIndex() != -1) {
      Attribute parameter = currentAttributes[annotationSelector.getSelectedIndex()];
      fetchValuesForAttribute(parameter);
    }
  }

  private void fetchValuesForAttribute(Attribute attribute) {
    int numMin = minorValues.size();
    parameterValues = new double[chosenCompounds.size()][numMin * 3];
    valuesRemaining = chosenCompounds.size() * numMin * 3;

    for (int c = 0; c < chosenCompounds.size(); ++c) {
      for (int d = 0; d < 3; ++d) {
        for (int t = 0; t < numMin; ++t) {
          final String compound = chosenCompounds.get(c);
          final String dose = mediumValues.get(d);

          final String time = minorValues.get(t);
          fetchValuesForAttribute(attribute, c, d * numMin + t, compound, dose, time);
        }
      }
    }
  }

  @Override
  protected Widget guiForUnit(Unit unit) {
    int time = minorValues.indexOf(unit.get(schema.timeParameter()));
    int compound = chosenCompounds.indexOf(unit.get(schema.majorParameter()));
    int dose = mediumValues.indexOf(unit.get(schema.mediumParameter()));
    HTML r = new HTML(unit.get(schema.timeParameter()));
    r.addStyleName("slightlySpaced");
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
