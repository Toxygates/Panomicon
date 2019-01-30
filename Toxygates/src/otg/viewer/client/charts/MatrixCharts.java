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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import otg.model.sample.OTGAttribute;
import otg.viewer.client.components.OTGScreen;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.CoreParameter;

/**
 * Entry point for constructing charts based on matrix data.
 */
public class MatrixCharts extends Charts {

  /**
   * Callbacks for a client that expects to receive an adjustable (interactive) chart.
   */
  public static interface Acceptor {
    void acceptCharts(AdjustableGrid<?, ?> cg);

    void acceptSamples(Sample[] samples);
  }

  private List<Group> groups;

  public MatrixCharts(OTGScreen screen, List<Group> groups) {
    super(screen);
    this.groups = groups;

    List<SampleClass> scs = new ArrayList<SampleClass>();
    for (Group g : groups) {
      for (Unit unit : g.getUnits()) {
        SampleClass unitClass = unit.getSamples()[0].sampleClass();
        SampleClass sc = SampleClassUtils.asMacroClass(unitClass, schema);
        sc.put(CoreParameter.ControlGroup, unitClass.get(CoreParameter.ControlGroup));
        scs.add(sc);
      }
    }

    this.sampleClasses = scs.toArray(new SampleClass[0]);
  }

  public ChartParameters parameters(ValueType vt, String title) {
    return new ChartParameters(screen, groups, vt, title);
  }

  /**
   * Make charts based on expression rows.
   */
  public void make(final ChartParameters params, final Sample[] samples, final String[] probes,
      final Acceptor acceptor) {
    String[] organisms = Group.collectAll(groups, OTGAttribute.Dataset).toArray(String[]::new);

    String[] majorVals = GroupUtils.collect(groups, schema.majorParameter()).toArray(String[]::new);

    // First, fetch data if we need to.

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
              finish(params, probes, result, acceptor);
            }
          });
    } else if (samples == null) {
      logger.info("Get rows for chart based on sample classes");
      sampleService.samples(sampleClasses, schema.majorParameter().id(), majorVals,
          new AsyncCallback<Sample[]>() {

            @Override
            public void onFailure(Throwable caught) {
              Window.alert("Unable to obtain chart data.");
              logger.log(Level.WARNING, "Unable to obtain chart data.", caught);
            }

            @Override
            public void onSuccess(final Sample[] samples) {
              finish(params, probes, samples, acceptor);
              /*
               * Note: the acceptor.acceptSamples control flow may not be the best way to structure
               * this
               */
              acceptor.acceptSamples(samples);
            }
          });
    } else {
      logger.info("Already had samples for chart");
      // We already have the necessary samples, can finish immediately
      finish(params, probes, samples, acceptor);
    }
  }

  /**
   * Complete a row chart by constructing the necessary dynamic data source, invoking the factory
   * method, and then sending the chart back to the acceptor.
   */
  private void finish(ChartParameters params, String[] probes, Sample[] samples,
      Acceptor acceptor) {
    DataSource dataSource =
        new DataSource.DynamicExpressionRowSource(schema, probes, samples, params.screen);
    logger.info("Finish charts with " + dataSource);
    AdjustableGrid<?, ?> acg = factory.adjustableGrid(params, dataSource);
    acceptor.acceptCharts(acg);
  }

  private void finish(ChartParameters params, String[] probes, Pair<Unit, Unit>[] units,
      Acceptor acceptor) {
    Set<Unit> treated = new HashSet<Unit>();
    for (Pair<Unit, Unit> u : units) {
      treated.add(u.first());
    }

    DataSource dataSource = new DataSource.DynamicUnitSource(schema, probes,
        treated.toArray(new Unit[0]), params.screen);
    logger.info("Finish charts with " + dataSource);
    AdjustableGrid<?, ?> acg = factory.adjustableGrid(params, dataSource);
    acceptor.acceptCharts(acg);
  }
}
