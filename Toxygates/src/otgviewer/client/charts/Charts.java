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

package otgviewer.client.charts;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import otg.model.sample.OTGAttribute;
import otgviewer.client.charts.ColorPolicy.TimeDoseColorPolicy;
import otgviewer.client.charts.google.GDTDataset;
import otgviewer.client.charts.google.GVizFactory;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.Series;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.CoreParameter;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.client.rpc.SeriesServiceAsync;

public class Charts {

  final static int DEFAULT_CHART_GRID_WIDTH = 600;
  
  public static interface ChartAcceptor {
    void acceptCharts(ChartGrid<?> cg);
  }

  public static interface AChartAcceptor {
    void acceptCharts(AdjustableGrid<?, ?> cg);

    void acceptBarcodes(Sample[] barcodes);
  }

  private final Logger logger = SharedUtils.getLogger("charts");

  protected final SampleServiceAsync sampleService;
  protected final SeriesServiceAsync seriesService;


  private SampleClass[] sampleClasses;
  private List<Group> groups;
  final private DataSchema schema;

  /*
   * Note: ideally this should be instantiated/chosen by some dependency injection system
   */
  private GVizFactory factory = new GVizFactory();
  private Screen screen;

  private Charts(Screen screen) {
    this.schema = screen.manager().schema();
    this.screen = screen;
    this.sampleService = screen.manager().sampleService();
    this.seriesService = screen.manager().seriesService();
  }

  public Charts(Screen screen, List<Group> groups) {
    this(screen);
    this.groups = groups;

    List<SampleClass> scs = new ArrayList<SampleClass>();
    for (Group g : groups) {
      for (Unit unit : g.getUnits()) {
        SampleClass unitClass = unit.getSamples()[0].sampleClass();
        SampleClass sc = SampleClassUtils.asMacroClass(unitClass,
            schema);
        sc.put(CoreParameter.ControlGroup, unitClass.get(CoreParameter.ControlGroup));
        scs.add(sc);
      }
    }

    this.sampleClasses = scs.toArray(new SampleClass[0]);

  }

  public Charts(Screen screen, SampleClass[] sampleClasses) {
    this(screen);
    this.sampleClasses = sampleClasses;
    groups = new ArrayList<Group>();
  }

  public ChartParameters parameters(ValueType vt, String title) {
    return new ChartParameters(screen, groups, vt, title);
  }

  public void makeSeriesCharts(final SeriesType seriesType, final List<Series> series, 
      final String highlightDoseOrTime, final ChartAcceptor acceptor, final Screen screen) {
    seriesService.expectedIndependentPoints(seriesType, series.get(0), 
        new PendingAsyncCallback<String[]>(screen,
        "Unable to obtain independent points for series.") {

      @Override
      public void handleSuccess(String[] result) {
        finishSeriesCharts(seriesType, series, result,  
            highlightDoseOrTime, acceptor, screen);
      }
    });
  }

  private void finishSeriesCharts(SeriesType seriesType, 
      final List<Series> series, final String[] indepPoints,
      final String highlightFixed, final ChartAcceptor acceptor,
      final Screen screen) {
    try {
      final String[] fixedVals = series.stream().
          map(s -> s.get(seriesType.fixedAttribute())).distinct().
          toArray(String[]::new);
      schema.sort(seriesType.fixedAttribute(), fixedVals);
      
      schema.sort(seriesType.independentAttribute(), indepPoints);
      DataSource cds = new DataSource.SeriesSource(schema, series, 
          seriesType.independentAttribute(), indepPoints);

      cds.getSamples(null, new SampleMultiFilter(), 
        new TimeDoseColorPolicy(highlightFixed, "SkyBlue"), 
        new DataSource.SampleAcceptor() {

        @Override
        public void accept(final List<ChartSample> samples) {
          boolean categoriesAreMinors = seriesType == SeriesType.Time;
          GDTDataset ds = factory.dataset(samples, indepPoints, categoriesAreMinors);
          List<String> filters = 
              series.stream().map(s -> s.probe()).distinct().collect(Collectors.toList());

          List<String> organisms =
                  new ArrayList<String>(
                      SampleClass.collect(Arrays.asList(sampleClasses), OTGAttribute.Organism));

          boolean columnsAreTimes = seriesType == SeriesType.Dose;
          ChartGrid<?> cg =
              factory.grid(screen, ds, filters, organisms, false,
                  fixedVals, columnsAreTimes, DEFAULT_CHART_GRID_WIDTH);
          cg.adjustAndDisplay(
            new ChartStyle(0, true, null, false),
            cg.getMaxColumnCount(), ds.getMin(), ds.getMax());
          acceptor.acceptCharts(cg);
        }

      });
    } catch (Exception e) {
      Window.alert("Unable to display charts: " + e.getMessage());
      logger.log(Level.WARNING, "Unable to display charts.", e);
    }
  }

  /**
   * Make charts based on expression rows.
   */
  public void makeRowCharts(final ChartParameters params, final Sample[] barcodes,
      final String[] probes, final AChartAcceptor acceptor) {
    String[] organisms = Group.collectAll(groups, OTGAttribute.Dataset).toArray(String[]::new);

    String[] majorVals =
        GroupUtils.collect(groups, schema.majorParameter()).toArray(String[]::new);

    if (organisms.length > 1) {
      logger.info("Get rows for chart based on units");
      sampleService.units(sampleClasses, schema.majorParameter().id(), majorVals,
          new AsyncCallback<Pair<Unit, Unit>[]>() {

            @Override
            public void onFailure(Throwable caught) {
              Window.alert("Unable to obtain chart data.");
              logger.log(Level.WARNING, "Unable to obtain chart data.", caught);
            }

            @Override
            public void onSuccess(Pair<Unit, Unit>[] result) {
              finishRowCharts(params, probes, result, acceptor);
            }
          });
    } else if (barcodes == null) {
      logger.info("Get rows for chart based on sample classes");
      sampleService.samples(sampleClasses, schema.majorParameter().id(), majorVals,
          new AsyncCallback<Sample[]>() {

            @Override
            public void onFailure(Throwable caught) {
              Window.alert("Unable to obtain chart data.");
              logger.log(Level.WARNING, "Unable to obtain chart data.", caught);
            }

            @Override
            public void onSuccess(final Sample[] barcodes) {
              finishRowCharts(params, probes, barcodes, acceptor);
              /*
               * Note: the acceptor.acceptBarcodes control flow may not be the best
               * way to structure this 
               */
              acceptor.acceptBarcodes(barcodes);
            }
          });
    } else {
      logger.info("Already had samples for chart");
      // We already have the necessary samples, can finish immediately
      finishRowCharts(params, probes, barcodes, acceptor);
    }
  }

  private void finishRowCharts(ChartParameters params, String[] probes, Sample[] barcodes,
      AChartAcceptor acceptor) {
    DataSource dataSource =
        new DataSource.DynamicExpressionRowSource(schema, probes, barcodes, params.screen);
    logger.info("Finish charts with " + dataSource);
    AdjustableGrid<?, ?> acg = factory.adjustableGrid(params, dataSource);
    acceptor.acceptCharts(acg);
  }

  private void finishRowCharts(ChartParameters params, String[] probes, Pair<Unit, Unit>[] units,
      AChartAcceptor acceptor) {
    Set<Unit> treated = new HashSet<Unit>();
    for (Pair<Unit, Unit> u : units) {
      treated.add(u.first());
    }

    DataSource dataSource =
        new DataSource.DynamicUnitSource(schema, probes, treated.toArray(new Unit[0]),
            params.screen);
    logger.info("Finish charts with " + dataSource);
    AdjustableGrid<?, ?> acg = factory.adjustableGrid(params, dataSource);
    acceptor.acceptCharts(acg);
  }
}
