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

package t.viewer.client.charts;

import com.google.gwt.user.client.Window;
import t.viewer.client.components.Screen;
import t.common.shared.FirstKeyedPair;
import t.common.shared.Pair;
import t.common.shared.SampleMultiFilter;
import t.common.shared.SeriesType;
import t.model.SampleClass;
import t.model.sample.OTGAttribute;
import t.viewer.client.charts.ColorPolicy.TimeDoseColorPolicy;
import t.viewer.client.charts.google.GDTDataset;
import t.viewer.client.components.PendingAsyncCallback;
import t.viewer.client.rpc.SeriesServiceAsync;
import t.viewer.shared.Series;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Entry point for making charts based on series data.
 */
public class SeriesCharts extends Charts {
  protected final SeriesServiceAsync seriesService;

  /**
   * Callback for a client that expects to receive a chart.
   */
  public interface Acceptor {
    void acceptCharts(ChartGrid<?> cg);
  }

  public SeriesCharts(Screen screen, SampleClass[] sampleClasses) {
    super(screen);
    this.sampleClasses = sampleClasses;
    this.seriesService = screen.manager().seriesService();
  }

  public void make(final SeriesType seriesType, final List<Series> series,
      final String highlightDoseOrTime, final Acceptor acceptor, final Screen screen,
      final String compoundName) {
    seriesService.expectedIndependentPoints(seriesType, series.get(0),
        new PendingAsyncCallback<String[]>(screen,
            "Unable to obtain independent points for series.", result -> finish(seriesType, series,
                result, highlightDoseOrTime, acceptor, screen, compoundName)));
  }

  private void finish(SeriesType seriesType, final List<Series> series, final String[] indepPoints,
      final String highlightFixed, final Acceptor acceptor, final Screen screen,
      final String compoundName) {
    try {
      final String[] fixedVals = series.stream().map(s -> s.get(seriesType.fixedAttribute()))
          .distinct().toArray(String[]::new);
      schema.sort(seriesType.fixedAttribute(), fixedVals);

      schema.sort(seriesType.independentAttribute(), indepPoints);
      DataSource dataSource = new DataSource.SeriesSource(schema, series,
          seriesType.independentAttribute(), indepPoints);

      List<DataPoint> points = dataSource.getPoints(null, new SampleMultiFilter(),
          new TimeDoseColorPolicy(highlightFixed, "SkyBlue"));

      boolean categoriesAreMinors = seriesType == SeriesType.Time;
      GDTDataset dataSet = factory.dataset(points, indepPoints, categoriesAreMinors, storageProvider);
      List<Pair<String, String>> filtersLabels =
          series.stream().map(s -> new FirstKeyedPair<String, String>(s.probe(), s.geneSym()))
              .distinct().collect(Collectors.toList());

      List<String> rowFilters =
          filtersLabels.stream().map(s -> s.first()).collect(Collectors.toList());
      List<String> rowLabels =
          filtersLabels.stream().map(s -> s.second() + "/" + s.first())
              .collect(Collectors.toList());

      List<String> organisms = new ArrayList<String>(
          SampleClass.collect(Arrays.asList(sampleClasses), OTGAttribute.Organism));

      boolean columnsAreTimes = seriesType == SeriesType.Dose;
      ChartGrid<?> cg = factory.grid(screen, dataSet, rowFilters, rowLabels, organisms, false, fixedVals,
          columnsAreTimes, DEFAULT_CHART_GRID_WIDTH);
      cg.adjustAndDisplay(new ChartStyle(0, true, null, false), cg.getMaxColumnCount(), dataSet.getMin(),
                  dataSet.getMax(), compoundName);
      acceptor.acceptCharts(cg);

    } catch (Exception e) {
      Window.alert("Unable to display charts: " + e);
      logger.log(Level.WARNING, "Unable to display charts.", e);
    }
  }
}
