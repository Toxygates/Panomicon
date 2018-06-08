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

package otgviewer.shared;

import java.util.*;

import otg.model.sample.OTGAttribute;
import t.common.shared.*;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.model.sample.CoreParameter;

@SuppressWarnings("serial")
public class OTGSchema extends DataSchema {
  public static String[] allTimes = new String[] {"2 hr", "3 hr", "6 hr", "8 hr", "9 hr", "24 hr",
      "4 day", "8 day", "15 day", "29 day"};
  public static String[] allDoses = new String[] {"Control", "Low", "Middle", "High"};

  @Override
  public String[] sortedValues(Attribute parameter) throws Exception {
    if (parameter.equals(OTGAttribute.ExposureTime)) {
      return allTimes;
    } else if (parameter.equals(OTGAttribute.DoseLevel)) {
      return allDoses;
    } else {
      throw new Exception("Invalid parameter (not sortable): " + parameter);
    }
  }

  @Override
  public String[] filterValuesForDisplay(ValueType vt, Attribute parameter, String[] from) {
    if (parameter.equals(OTGAttribute.DoseLevel) && (vt == null || vt == ValueType.Folds)) {
      ArrayList<String> r = new ArrayList<String>();
      for (String s : from) {
        if (!isControlValue(s)) {
          r.add(s);
        }
      }
      return r.toArray(new String[0]);
    } else {
      return super.filterValuesForDisplay(vt, parameter, from);
    }
  }

  @Override
  public void sort(Attribute parameter, String[] values) throws Exception {
    if (parameter.equals(OTGAttribute.ExposureTime)) {
      sortTimes(values);
    } else {
      super.sort(parameter, values);
    }
  }

  public void sortDoses(String[] doses) throws Exception {
    sort(OTGAttribute.DoseLevel, doses);
  }

  // TODO validity check units and exposure times before we subject
  // them to this parsing (and report errors)

  private int toMinutes(int n, String unit) {
    if (unit.equals("min")) {
      return n;
    } else if (unit.equals("hr")) {
      return n * 60;
    } else if (unit.equals("day")) {
      return n * 24 * 60;
    } else if (unit.equals("week")) {
      return n * 7 * 24 * 60;
    } else {
      throw new IllegalArgumentException("Unknown time unit " + unit);
    }
  }

  @Override
  public void sortTimes(String[] times) throws Exception {
    Arrays.sort(times, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        // Examples: "9 day" and "10 hr";

        String[] s1 = o1.split("\\s+"), s2 = o2.split("\\s");
        if (s1.length < 2 || s2.length < 2) {
          throw new IllegalArgumentException("Format error: unable to discover time "
              + "unit in strings " + s1 + " and " + s2);
        }
        String u1 = s1[1], u2 = s2[1];
        int v1 = Integer.valueOf(s1[0]), v2 = Integer.valueOf(s2[0]);
        int n1 = toMinutes(v1, u1), n2 = toMinutes(v2, u2);
        if (n1 < n2) {
          return -1;
        } else if (n1 == n2) {
          return 0;
        } else {
          return 1;
        }
      }
    });
  }

  @Override
  public Attribute majorParameter() {
    return OTGAttribute.Compound;
  }

  @Override
  public Attribute mediumParameter() {
    return OTGAttribute.DoseLevel;
  }

  @Override
  public Attribute minorParameter() {
    return OTGAttribute.ExposureTime;
  }

  @Override
  public Attribute timeParameter() {
    return OTGAttribute.ExposureTime;
  }

  @Override
  public Attribute timeGroupParameter() {
    return OTGAttribute.DoseLevel;
  }

  private Attribute[] macroParams = new Attribute[] {
      CoreParameter.Type,
      OTGAttribute.Organism, OTGAttribute.TestType,
      OTGAttribute.Organ, OTGAttribute.Repeat};

  @Override
  public Attribute[] macroParameters() {
    return macroParams;
  }

  @Override
  public boolean isSelectionControl(SampleClass sc) {
    return sc.get(OTGAttribute.DoseLevel).equals("Control");
  }

  @Override
  public boolean isControlValue(String value) {
    return (value != null) && ("Control".equals(value));
  }

  @Override
  @Deprecated
  public String[] majorParamSharedControl() {
    return new String[] {"Shared_control"};
  }

  private static AType[] associations = new AType[] {AType.Chembl, AType.Drugbank,
      // AType.Enzymes,
      AType.GOBP,
      AType.GOCC,
      AType.GOMF,
      // needs repair
      // AType.Homologene,
      AType.KEGG,
      // needs repair
      // AType.OrthProts,
      AType.Uniprot, AType.RefseqTrn, AType.RefseqProt, AType.EC, AType.Ensembl, AType.Unigene,
      AType.MiRNA, AType.MRNA};

  @Override
  public AType[] associations() {
    return associations;
  }

  /**
   * TODO: this is brittle, given that platform names may change externally - think about a better
   * way of doing this long term
   */
  public String platformOrganism(String platform) {
    if (platform.startsWith("HG")) {
      return "Human";
    } else if (platform.startsWith("Rat")) {
      return "Rat";
    } else if (platform.startsWith("Mouse")) {
      return "Mouse";
    } else {
      return super.platformSpecies(platform);
    }
  }

  // TODO as above.
  @Override
  public String organismPlatform(String organism) {
    if (organism.equals("Human")) {
      return "HG-U133_Plus_2";
    } else if (organism.equals("Rat")) {
      return "Rat230_2";
    } else if (organism.equals("Mouse")) {
      return "Mouse430_2";
    } else {
      return null;
    }
  }

  /**
   * TODO this is brittle
   */
  @Override
  public int numDataPointsInSeries(SampleClass sc) {
    if (sc.get(OTGAttribute.TestType) != null &&
        sc.get(OTGAttribute.TestType).equals("in vitro")) {
      return 3;
    }
    return 4;
  }

  @Override
  public Attribute[] chartParameters() {
    return new Attribute[] {minorParameter(), mediumParameter(), majorParameter(),
        OTGAttribute.Organism};
  }
}
