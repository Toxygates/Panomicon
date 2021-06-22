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

package t.common.shared;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

import javax.annotation.Nullable;

import t.common.shared.sample.Sample;
import t.model.SampleClass;
import t.model.sample.Attribute;


/**
 * Information about the data schema in a particular T application.
 */
@SuppressWarnings("serial")
public abstract class DataSchema implements Serializable {

  Attribute[] defaultChartParameters = new Attribute[3];

  public DataSchema() {
    defaultChartParameters[0] = majorParameter();
    defaultChartParameters[1] = mediumParameter();
    defaultChartParameters[2] = minorParameter();
  }

  /**
   * All the possible values, in their natural order, for a sortable parameter.
   */
  public abstract String[] sortedValues(Attribute parameter) throws Exception;

  /**
   * Ordered values for a sortable parameter, which should be displayed to the user in the context
   * of a given list of sample classes.
   */
  public String[] sortedValuesForDisplay(@Nullable ValueType vt, Attribute parameter)
      throws Exception {
    return filterValuesForDisplay(vt, parameter, sortedValues(parameter));
  }

  public String[] filterValuesForDisplay(@Nullable ValueType vt, Attribute parameter,
      String[] from) {
    return from;
  }

  /**
   * Sort values from the given sortable parameter in place.
   */
  public void sort(Attribute parameter, String[] values) throws Exception {
    final String[] sorted = sortedValues(parameter);
    Arrays.sort(values, new Comparator<String>() {
      @Override
      public int compare(String e1, String e2) {
        Integer i1 = SharedUtils.indexOf(sorted, e1);
        Integer i2 = SharedUtils.indexOf(sorted, e2);
        return i1.compareTo(i2);
      }
    });
  }

  public void sortTimes(String[] times) throws Exception {
    sort(timeParameter(), times);
  }

  /**
   * Used in the "compound list", and other places
   * 
   * @return
   */
  public abstract Attribute majorParameter();

  /**
   * Used for columns in the time/dose selection grid
   * 
   * @return
   */
  public abstract Attribute mediumParameter();

  /**
   * Used for subcolumns (checkboxes) in the time/dose selection grid
   * 
   * @return
   */
  public abstract Attribute minorParameter();

  /**
   * Used for charts
   * 
   * @return
   */
  public abstract Attribute timeParameter();

  /**
   * Used to group charts in columns, when possible
   * 
   * @return
   */
  public abstract Attribute timeGroupParameter();

  public abstract Attribute[] macroParameters();

  public Attribute[] chartParameters() {
    return defaultChartParameters;
  }

  public boolean isSelectionControl(SampleClass sc) {
    return false;
  }

  public boolean isControlValue(String parameter, String value) {
    return (parameter.equals(mediumParameter().id()) && isControlValue(value))
        || (parameter.equals(majorParameter().id()) && isMajorParamSharedControl(value));
  }

  /**
   * Is the value a control value for the medium parameter?
   */
  public boolean isControlValue(String value) {
    return false;
  }

  public boolean isControl(SampleClass s) {
    return isControlValue(s.get(mediumParameter()));
  }

  /**
   * Whether the value is a "shared control" quasi-compound name
   * that can be hidden from the user in some situations.
   */
  public boolean isMajorParamSharedControl(String value) {
    String[] mpvs = majorParamSharedControl();
    if (mpvs == null) {
      return false;
    }
    for (String v : mpvs) {
      if (v.equals(value)) {
        return true;
      }
    }
    return false;
  }

  @Deprecated
  protected @Nullable String[] majorParamSharedControl() {
    return new String[] {};
  }

  public AType[] associations() {
    return new AType[] {};
  }

  public String getMinor(HasClass hc) {
    return hc.sampleClass().get(minorParameter());
  }

  public String getMedium(HasClass hc) {
    return hc.sampleClass().get(mediumParameter());
  }

  public String getMajor(HasClass hc) {
    return hc.sampleClass().get(majorParameter());
  }

  public abstract int numDataPointsInSeries(SampleClass sc, SeriesType st);

  // Note: should move down to otg
  @Nullable
  public String organismPlatform(String organism) {
    return null;
  }

  @Nullable
  public String chartLabel(HasClass hc) {
    return null;
  }

  @Nullable
  public String suggestedColor(HasClass hc) {
    return null;
  }
}
