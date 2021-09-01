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

package t.gwt.viewer.client.charts;

import t.gwt.viewer.client.screen.Screen;
import t.shared.common.GroupUtils;
import t.shared.common.Pair;
import t.shared.common.ValueType;
import t.shared.common.sample.Group;
import t.shared.common.sample.Sample;
import t.shared.common.sample.SampleClassUtils;
import t.shared.common.sample.Unit;
import t.model.SampleClass;
import t.model.sample.CoreParameter;
import t.model.sample.OTGAttribute;
import t.gwt.viewer.client.ClientGroup;
import t.gwt.viewer.client.charts.DataSource.DynamicExpressionRowSource;
import t.gwt.viewer.client.charts.DataSource.DynamicUnitSource;
import t.gwt.viewer.client.charts.DataSource.ExpressionRowSource;
import t.gwt.viewer.client.components.PendingAsyncCallback;
import t.gwt.viewer.client.rpc.SampleServiceAsync;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entry point for constructing charts based on matrix data.
 */
public class MatrixCharts extends Charts {

  /**
   * Callback for a client that expects to receive an adjustable (interactive) chart.
   */
  public interface Acceptor {
    void accept(AdjustableGrid<?> cg);
  }

  protected final SampleServiceAsync sampleService;
  private List<ClientGroup> groups;

  /**
   * Samples will be fetched based on the sample classes of the groups the first time they are
   * needed (since for additional context, we may need samples that are not in the groups, e.g.
   * other doses and times). They will then be cached for further use.
   */
  private Sample[] samples;

  /**
   * Construct MatrixCharts.
   * 
   * @param screen The screen that will display the charts.
   * @param groups The user's selected groups. Based on these, additional surrounding sample classes
   *        will also be included to give context in the charts.
   */
  public MatrixCharts(Screen screen, List<ClientGroup> groups) {
    super(screen);
    this.groups = groups;
    this.sampleService = screen.manager().sampleService();

    List<SampleClass> scs = new ArrayList<SampleClass>();
    for (Group group : groups) {
      for (Unit unit : group.getUnits()) {
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
  public void make(final ChartParameters params, final String[] probes, final Acceptor acceptor) {
    String[] organisms = Group.collectAll(groups, OTGAttribute.Organism).toArray(String[]::new);

    String[] majorVals = GroupUtils.collect(groups, schema.majorParameter()).toArray(String[]::new);

    // First, fetch data if we need to.

    if (organisms.length > 1) {
      logger.info("Get rows for chart based on units");
      sampleService.units(sampleClasses, schema.majorParameter().id(), majorVals,
          new PendingAsyncCallback<Pair<Unit, Unit>[]>(screen.manager(), "Unable to obtain chart data",
              result -> finish(params, probes, result, acceptor)));

    } else if (samples == null) {
      logger.info("Get rows for chart based on sample classes");
      sampleService.samples(sampleClasses, schema.majorParameter().id(), majorVals,
          new PendingAsyncCallback<Sample[]>(screen.manager(), "Unable to obtain chart data",
              samples -> {
                finish(params, probes, samples, acceptor);
                MatrixCharts.this.samples = samples;
              }));
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
    ExpressionRowSource dataSource =
        new DynamicExpressionRowSource(schema, probes, samples, params.screen);
    logger.info("Finish charts with " + dataSource);
    AdjustableGrid<?> acg = factory.adjustableGrid(params, dataSource);
    acceptor.accept(acg);
  }

  private void finish(ChartParameters params, String[] probes, Pair<Unit, Unit>[] units,
      Acceptor acceptor) {
    Set<Unit> treated = new HashSet<Unit>();
    for (Pair<Unit, Unit> u : units) {
      treated.add(u.first());
    }

    ExpressionRowSource dataSource =
        new DynamicUnitSource(schema, probes, treated.toArray(new Unit[0]), params.screen);
    logger.info("Finish charts with " + dataSource);
    AdjustableGrid<?> acg = factory.adjustableGrid(params, dataSource);
    acceptor.accept(acg);
  }
}
