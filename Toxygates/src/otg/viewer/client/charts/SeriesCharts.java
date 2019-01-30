/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package otg.viewer.client.charts;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.gwt.user.client.Window;

import otg.model.sample.OTGAttribute;
import otg.viewer.client.charts.ColorPolicy.TimeDoseColorPolicy;
import otg.viewer.client.charts.google.GDTDataset;
import otg.viewer.client.components.OTGScreen;
import otg.viewer.shared.Series;
import t.common.shared.SampleMultiFilter;
import t.common.shared.SeriesType;
import t.model.SampleClass;
import t.viewer.client.components.PendingAsyncCallback;

/**
 * Entry point for making charts based on series data.
 */
public class SeriesCharts extends Charts {

  /**
   * Callback for a client that expects to receive a chart.
   */
  public static interface Acceptor {
    void acceptCharts(ChartGrid<?> cg);
  }

  public SeriesCharts(OTGScreen screen, SampleClass[] sampleClasses) {
    super(screen);
    this.sampleClasses = sampleClasses;
  }

  public void make(final SeriesType seriesType, final List<Series> series,
      final String highlightDoseOrTime, final Acceptor acceptor, final OTGScreen screen) {
    seriesService.expectedIndependentPoints(seriesType, series.get(0),
        new PendingAsyncCallback<String[]>(screen,
            "Unable to obtain independent points for series.") {

          @Override
          public void handleSuccess(String[] result) {
            finish(seriesType, series, result, highlightDoseOrTime, acceptor, screen);
          }
        });
  }

  private void finish(SeriesType seriesType, final List<Series> series, final String[] indepPoints,
      final String highlightFixed, final Acceptor acceptor, final OTGScreen screen) {
    try {
      final String[] fixedVals = series.stream().map(s -> s.get(seriesType.fixedAttribute()))
          .distinct().toArray(String[]::new);
      schema.sort(seriesType.fixedAttribute(), fixedVals);

      schema.sort(seriesType.independentAttribute(), indepPoints);
      DataSource cds = new DataSource.SeriesSource(schema, series,
          seriesType.independentAttribute(), indepPoints);

      cds.getSamples(null, new SampleMultiFilter(),
          new TimeDoseColorPolicy(highlightFixed, "SkyBlue"), new DataSource.SampleAcceptor() {

            @Override
            public void accept(final List<ChartSample> samples) {
              boolean categoriesAreMinors = seriesType == SeriesType.Time;
              GDTDataset ds =
                  factory.dataset(samples, indepPoints, categoriesAreMinors, storageProvider);
              List<String> filters =
                  series.stream().map(s -> s.probe()).distinct().collect(Collectors.toList());

              List<String> organisms = new ArrayList<String>(
                  SampleClass.collect(Arrays.asList(sampleClasses), OTGAttribute.Organism));

              boolean columnsAreTimes = seriesType == SeriesType.Dose;
              ChartGrid<?> cg = factory.grid(screen, ds, filters, organisms, false, fixedVals,
                  columnsAreTimes, DEFAULT_CHART_GRID_WIDTH);
              cg.adjustAndDisplay(new ChartStyle(0, true, null, false), cg.getMaxColumnCount(),
                  ds.getMin(), ds.getMax());
              acceptor.acceptCharts(cg);
            }

          });
    } catch (Exception e) {
      Window.alert("Unable to display charts: " + e.getMessage());
      logger.log(Level.WARNING, "Unable to display charts.", e);
    }
  }
}
