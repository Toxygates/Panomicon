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

package otgviewer.shared;

import static otg.model.sample.OTGAttribute.*;

import java.io.Serializable;

import otg.model.sample.OTGAttribute;
import t.common.shared.HasClass;
import t.common.shared.sample.ExpressionValue;
import t.model.SampleClass;
import t.model.sample.Attribute;

/**
 * An expression value series that fixes all parameters except one, which varies on the x-axis.
 */
@SuppressWarnings("serial")
public class Series implements HasClass, Serializable {

  public Series() {}

  /**
   * Construct a new series.
   * 
   * @param title Description of this series
   * @param probe
   * @param independentParam The parameter that is varied on the x-axis. For example, in OTG, if
   *        exposure_time is independent, then this is a time series.
   * @param values Data values for this series (ordered by dose if the time is fixed, or ordered by
   *        time if the dose is fixed)
   * @param sc Sample class parameters
   * @param values Data points
   */
  public Series(String title, String probe, OTGAttribute independentParam, SampleClass sc,
      ExpressionValue[] values) {
    _values = values;
    _title = title;
    _probe = probe;
    _independentParam = independentParam;
    _sc = sc;
  }

  private SampleClass _sc;

  @Override
  public SampleClass sampleClass() {
    return _sc;
  }

  private String _probe;

  public String probe() {
    return _probe;
  }

  private OTGAttribute _independentParam;

  public OTGAttribute independentParam() {
    return _independentParam;
  }

  // TODO users should access the sample class instead
  @Deprecated
  public String timeDose() {
    Attribute fixedParam = _independentParam == ExposureTime ? DoseLevel : ExposureTime;
    return _sc.get(fixedParam);
  }

  private ExpressionValue[] _values;

  public ExpressionValue[] values() {
    return _values;
  }

  private String _title;

  public String title() {
    return _title;
  }

  public String get(Attribute key) {
    return _sc.get(key);
  }
}
