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

package t.viewer.client.table;

/**
 * Controls the overall style of an ExpressionTable.
 */
public abstract class TableStyle {
  abstract boolean initVisibility(StandardColumns col);
  abstract String initWidth(StandardColumns col);
  
  public static TableStyle getStyle(String name) {
    if (name.equals("miRNA")) {
      return new MirnaTableStyle();
    } else {
      return new DefaultTableStyle();
    }
  }
  
  static class DefaultTableStyle extends TableStyle {
    boolean initVisibility(StandardColumns col) {
      return col != StandardColumns.GeneID;
    }

    String initWidth(StandardColumns col) {
      switch (col) {
        case Probe:
          return "8em";
        case GeneSym:
          return "10em";
        case ProbeTitle:
          return "18em";
        case GeneID:
          return "12em";
        default:
          return "15em";
      }
    }   
  }
  
  static class MirnaTableStyle extends DefaultTableStyle {
    @Override
    String initWidth(StandardColumns col) {
      switch (col) {
        case Probe:
          return "10em";
        default:
          return super.initWidth(col);
      }
    }
    
    @Override
    boolean initVisibility(StandardColumns col) {
      return col == StandardColumns.Probe;
    }
  }
}
