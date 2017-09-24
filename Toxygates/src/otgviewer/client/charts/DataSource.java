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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.Series;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.shared.FullMatrix;

/**
 * This class brings series and row data into a unified interface for the purposes of chart drawing.
 * TODO: simplify
 */
abstract public class DataSource {

  interface SampleAcceptor {
    void accept(List<ChartSample> samples);
  }

  private static Logger logger = SharedUtils.getLogger("chartdata");

  protected List<ChartSample> chartSamples = new ArrayList<ChartSample>();

  protected String[] _minorVals;
  protected String[] _mediumVals;

  String[] minorVals() {
    return _minorVals;
  }

  String[] mediumVals() {
    return _mediumVals;
  }

  protected DataSchema schema;
  protected boolean controlMedVals = false;

  DataSource(DataSchema schema) {
    this.schema = schema;
  }

  protected void initParams(List<? extends HasClass> from, boolean controlMedVals) {
    try {
      Attribute minorParam = schema.minorParameter();
      Attribute medParam = schema.mediumParameter();
      Set<String> minorVals = SampleClassUtils.collectInner(from, minorParam);
      _minorVals = minorVals.toArray(new String[0]);
      schema.sort(minorParam, _minorVals);

      Set<String> medVals = new HashSet<String>();
      for (HasClass f : from) {
        // TODO generalise control-check better
        if (controlMedVals || !schema.isControlValue(schema.getMedium(f))) {
          medVals.add(schema.getMedium(f));
        }
      }
      _mediumVals = medVals.toArray(new String[0]);
      schema.sort(medParam, _mediumVals);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Unable to sort chart data", e);
    }
  }

  private void applyPolicy(ColorPolicy policy, List<ChartSample> samples) {
    for (ChartSample s : samples) {
      s.color = policy.colorFor(s);
    }
  }

  /**
   * Obtain samples, making an asynchronous call if necessary, and pass them on to the sample
   * acceptor when they are available.
   */
  void getSamples(SampleMultiFilter smf, ColorPolicy policy, SampleAcceptor acceptor) {
    if (!smf.contains(schema.majorParameter())) {
      // TODO why is this needed?
      applyPolicy(policy, chartSamples);
      acceptor.accept(chartSamples);
    } else {
      // We store these in a set since we may be getting the same samples several times
      Set<ChartSample> r = new HashSet<ChartSample>();
      for (ChartSample s : chartSamples) {
        if (smf.accepts(s)) {
          r.add(s);
          s.color = policy.colorFor(s);
        }
      }
      acceptor.accept(new ArrayList<ChartSample>(r));
    }
  }

  static class SeriesSource extends DataSource {
    SeriesSource(DataSchema schema, List<Series> series, String[] times) {
      super(schema);
      for (Series s : series) {
        for (int i = 0; i < s.values().length; ++i) {
          ExpressionValue ev = s.values()[i];
          String time = times[i];
          SampleClass sc = s.sampleClass().copyWith(schema.timeParameter(), time);
          ChartSample cs =
              new ChartSample(sc, schema, ev.getValue(), null, s.probe(), ev.getCall(), null);
          chartSamples.add(cs);
        }
      }
      initParams(chartSamples, false);
    }
  }

  /**
   * An expression row source with a fixed dataset.
   */
  static class ExpressionRowSource extends DataSource {
    protected Sample[] samples;

    ExpressionRowSource(DataSchema schema, Sample[] samples, List<ExpressionRow> rows) {
      super(schema);
      this.samples = samples;
      addSamplesFromBarcodes(samples, rows);
      initParams(Arrays.asList(samples), true);
    }

    protected void addSamplesFromBarcodes(Sample[] samples, List<ExpressionRow> rows) {
      logger.info("Add samples from " + samples.length + " samples and " + rows.size() + " rows");
      for (int i = 0; i < samples.length; ++i) {
        for (ExpressionRow er : rows) {
          ExpressionValue ev = er.getValue(i);
          ChartSample cs =
              new ChartSample(samples[i], schema, ev.getValue(), er.getProbe(), ev.getCall(),
                  schema.chartLabel(samples[i]));
          chartSamples.add(cs);
        }
      }
    }
  }

  /**
   * An expression row source that dynamically loads data.
   */
  static class DynamicExpressionRowSource extends ExpressionRowSource {
    protected final MatrixServiceAsync matrixService;

    protected String[] probes;
    protected ValueType type;
    protected Screen screen;

    DynamicExpressionRowSource(DataSchema schema, String[] probes, ValueType vt, Sample[] barcodes,
        Screen screen) {
      super(schema, barcodes, new ArrayList<ExpressionRow>());
      this.probes = probes;
      this.type = vt;
      this.screen = screen;
      this.matrixService = screen.manager().matrixService();
    }

    void loadData(final SampleMultiFilter smf, final ColorPolicy policy,
        final SampleAcceptor acceptor) {
      logger.info("Dynamic source: load for " + smf);

      final List<Sample> useSamples = new ArrayList<Sample>();
      for (Sample b : samples) {
        if (smf.accepts(b)) {
          useSamples.add(b);
        }
      }

      chartSamples.clear();
      Group g = new Group(schema, "temporary", useSamples.toArray(new Sample[0]));
      List<Group> gs = new ArrayList<Group>();
      gs.add(g);
      matrixService.getFullData(gs, probes, false, type,
          new PendingAsyncCallback<FullMatrix>(screen, "Unable to obtain chart data.") {

            @Override
            public void handleSuccess(final FullMatrix mat) {
              addSamplesFromBarcodes(useSamples.toArray(new Sample[0]), mat.rows());
              getLoadedSamples(smf, policy, acceptor);
            }
          });

    }

    // TODO think about the way these methods interact with superclass
    // - bad design
    @Override
    void getSamples(SampleMultiFilter smf, ColorPolicy policy, SampleAcceptor acceptor) {
      loadData(smf, policy, acceptor);
    }

    protected void getLoadedSamples(SampleMultiFilter smf, ColorPolicy policy,
        SampleAcceptor acceptor) {
      super.getSamples(smf, policy, acceptor);
    }
  }

  /**
   * A dynamic source that makes requests based on a list of units.
   */
  static class DynamicUnitSource extends DynamicExpressionRowSource {
    private Unit[] units;

    DynamicUnitSource(DataSchema schema, String[] probes, ValueType vt, Unit[] units, Screen screen) {
      super(schema, probes, vt, Unit.collectBarcodes(units), screen);
      this.units = units;
    }

    @Override
    void loadData(final SampleMultiFilter smf, final ColorPolicy policy,
        final SampleAcceptor acceptor) {
      logger.info("Dynamic unit source: load for " + smf);

      final List<Group> groups = new ArrayList<Group>();
      final List<Unit> useUnits = new ArrayList<Unit>();
      int i = 0;
      for (Unit u : units) {
        if (smf.accepts(u)) {
          Group g = new Group(schema, "g" + i, u.getSamples());
          i++;
          groups.add(g);
          useUnits.add(u);
        }
      }

      chartSamples.clear();
      matrixService.getFullData(groups, probes, false, type,
          new PendingAsyncCallback<FullMatrix>(screen, "Unable to obtain chart data") {

            @Override
            public void handleSuccess(final FullMatrix mat) {
              addSamplesFromUnits(useUnits, mat.rows());
              getLoadedSamples(smf, policy, acceptor);
            }
          });
    }

    protected void addSamplesFromUnits(List<Unit> units, List<ExpressionRow> rows) {
      logger.info("Add samples from " + units.size() + " units and " + rows.size() + " rows");
      for (int i = 0; i < units.size(); ++i) {
        for (ExpressionRow er : rows) {
          ExpressionValue ev = er.getValue(i);
          ChartSample cs =
              new ChartSample(units.get(i), schema, ev.getValue(), er.getProbe(), ev.getCall());
          chartSamples.add(cs);
        }
      }
    }
  }

}
