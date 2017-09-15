/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import static t.model.sample.CoreParameter.ControlGroup;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import otgviewer.client.charts.ColorPolicy.TimeDoseColorPolicy;
import otgviewer.client.charts.google.GDTDataset;
import otgviewer.client.charts.google.GVizFactory;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.Series;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.client.rpc.SeriesServiceAsync;

public class Charts {

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
  // TODO where to instantiate this?
  private GVizFactory factory = new GVizFactory();

  private Charts(Screen screen) {
    this.schema = screen.schema();
    this.sampleService = screen.manager().sampleService();
    this.seriesService = screen.manager().seriesService();
  }

  public Charts(Screen screen, List<Group> groups) {
    this(screen);
    this.groups = groups;

    List<SampleClass> scs = new ArrayList<SampleClass>();
    for (Group g : groups) {
      SampleClass groupSc = g.getSamples()[0].sampleClass();
      SampleClass sc = SampleClassUtils.asMacroClass(groupSc,
          schema);
      sc.put(ControlGroup, groupSc.get(ControlGroup));
      scs.add(sc);
    }

    this.sampleClasses = scs.toArray(new SampleClass[0]);

  }

  public Charts(Screen screen, SampleClass[] sampleClasses) {
    this(screen);
    this.sampleClasses = sampleClasses;
    groups = new ArrayList<Group>();
  }

  public void makeSeriesCharts(final List<Series> series, final boolean rowsAreCompounds,
      final int highlightDose, final ChartAcceptor acceptor, final Screen screen) {
    seriesService.expectedTimes(series.get(0), new PendingAsyncCallback<String[]>(screen,
        "Unable to obtain sample times.") {

      @Override
      public void handleSuccess(String[] result) {
        finishSeriesCharts(series, result, rowsAreCompounds, highlightDose, acceptor, screen);
      }
    });
  }

  private void finishSeriesCharts(final List<Series> series, final String[] times,
      final boolean rowsAreCompounds, final int highlightMed, final ChartAcceptor acceptor,
      final Screen screen) {
    // TODO get from schema or data
    try {
      final Attribute majorParam = schema.majorParameter();
      final String[] medVals = schema.sortedValuesForDisplay(null, schema.mediumParameter().id());
      schema.sort(schema.timeParameter().id(), times);
      DataSource cds = new DataSource.SeriesSource(schema, series, times);

      cds.getSamples(new SampleMultiFilter(), new TimeDoseColorPolicy(medVals[highlightMed],
          "SkyBlue"), new DataSource.SampleAcceptor() {

        @Override
        public void accept(final List<ChartSample> samples) {
          GDTDataset ds = factory.dataset(samples, samples, times, true);
          List<String> filters = new ArrayList<String>();
          for (Series s : series) {
            if (rowsAreCompounds && !filters.contains(s.get(majorParam))) {
              filters.add(s.get(majorParam));
            } else if (!filters.contains(s.probe())) {
              filters.add(s.probe());
            }
          }

          List<String> organisms =
              new ArrayList<String>(SampleClass.collect(Arrays.asList(sampleClasses), "organism"));

          ChartGrid<?> cg =
              factory.grid(screen, ds, filters, organisms, rowsAreCompounds, medVals, false, 400);
          cg.adjustAndDisplay(cg.getMaxColumnCount(), ds.getMin(), ds.getMax());
          acceptor.acceptCharts(cg);
        }

      });
    } catch (Exception e) {
      Window.alert("Unable to display charts: " + e.getMessage());
      logger.log(Level.WARNING, "Unable to display charts.", e);
    }
  }

  public void makeRowCharts(final Screen screen, final Sample[] barcodes, final ValueType vt,
      final String[] probes, final AChartAcceptor acceptor) {
    Set<String> organisms = Group.collectAll(groups, "organism");

    String[] majorVals =
        GroupUtils.collect(groups, schema.majorParameter().id()).toArray(new String[0]);

    if (organisms.size() > 1) {
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
              finishRowCharts(screen, probes, vt, groups, result, acceptor);
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
              finishRowCharts(screen, probes, vt, groups, barcodes, acceptor);
              // TODO is this needed/well designed?
              acceptor.acceptBarcodes(barcodes);
            }
          });
    } else {
      logger.info("Already had samples for chart");
      // We already have the necessary samples, can finish immediately
      finishRowCharts(screen, probes, vt, groups, barcodes, acceptor);
    }
  }

  private void finishRowCharts(Screen screen, String[] probes, ValueType vt, List<Group> groups,
      Sample[] barcodes, AChartAcceptor acceptor) {
    DataSource dataSource =
        new DataSource.DynamicExpressionRowSource(schema, probes, vt, barcodes, screen);
    logger.info("Finish charts with " + dataSource);
    AdjustableGrid<?, ?> acg = factory.adjustableGrid(screen, dataSource, groups, vt);
    acceptor.acceptCharts(acg);
  }

  private void finishRowCharts(Screen screen, String[] probes, ValueType vt, List<Group> groups,
      Pair<Unit, Unit>[] units, AChartAcceptor acceptor) {
    Set<Unit> treated = new HashSet<Unit>();
    for (Pair<Unit, Unit> u : units) {
      treated.add(u.first());
    }

    DataSource dataSource =
        new DataSource.DynamicUnitSource(schema, probes, vt, treated.toArray(new Unit[0]), screen);
    logger.info("Finish charts with " + dataSource);
    AdjustableGrid<?, ?> acg = factory.adjustableGrid(screen, dataSource, groups, vt);
    acceptor.acceptCharts(acg);
  }
}
