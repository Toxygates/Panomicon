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

package t.shared.viewer.network;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import t.shared.common.SharedUtils;
import t.shared.common.sample.ExpressionRow;
import t.shared.common.sample.ExpressionValue;
import t.shared.viewer.ManagedMatrixInfo;

@SuppressWarnings("serial")
public class Node implements Serializable {
  String id, type;
  List<String> symbols;
  HashMap<String, Double> weights;
  
  //GWT constructor
  Node() {}

  //Note: it is probably desirable to maintain symbols consistently inside each ExpressionRow,
  //removing the need to pass them in from outside here.
  //This may be done as part of removing/refactoring RowAnnotation
  public static Node fromRow(ExpressionRow row, List<String> geneSymbols, String type,
      ManagedMatrixInfo matrixInfo) {
	  
//    String[] geneSymbols = row.getGeneSyms();
    ExpressionValue[] values = row.getValues();

    // Exclude synthetic columns (e.g. the count column for side matrices)
    int numDataColumns = matrixInfo.numDataColumns();
    Map<String, Double> weights = IntStream.range(0, numDataColumns).boxed()
        .collect(Collectors.toMap(i -> matrixInfo.columnName(i), i -> values[i].getValue()));

    if (geneSymbols == null) {
      geneSymbols = new ArrayList<String>();
    }
    return new Node(row.getProbe(), geneSymbols, type, new HashMap<String, Double>(weights));
  }
  
  public Node(String id, List<String> symbols, String type, HashMap<String, Double> weights) {
    this.id = id;
    this.symbols = symbols;
    this.type = type;
    this.weights = weights;
  }
  
  @Override
  public int hashCode() {
    return id.hashCode();
  }
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof Node) {
      return id.equals(((Node)other).id);
    }
    return false;
  }
  
  public String id() { return id; }
  public String type() { return type; }

  public Map<String, Double> weights() {
    return weights;
  }
  public List<String> symbols() {
    return symbols;
  }
  
  /**
   * Convenience method to get all symbols as a slash-separated string.
   */
  public String symbolString() {
    if (symbols != null) {
      return SharedUtils.mkString(symbols, "/");
    } else {
      return "";
    }
  }

}
