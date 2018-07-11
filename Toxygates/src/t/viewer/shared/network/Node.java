package t.viewer.shared.network;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import t.common.shared.SharedUtils;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.ExpressionValue;

@SuppressWarnings("serial")
public class Node implements Serializable {
  String id, type;
  List<String> symbols;
  HashMap<String, Double> weights;
  
  //GWT constructor
  Node() {}
  
  public interface ColumnNameProvider {
    public String get(int index);
  }

  public static Node fromRow(ExpressionRow row, String type, ColumnNameProvider columnNames) {
    // When there's no gene symbol, for some reason row.geneSymbols gives us a singleton
    // list with an empty string, which we don't want here.
    String[] geneSymbols = row.getGeneSyms();
    if (geneSymbols.length == 1 && geneSymbols[0] == "") {
      geneSymbols = new String[0];
    }
    ExpressionValue[] values = row.getValues();
    Map<String, Double> weights = IntStream.range(0, values.length).boxed()
        .collect(Collectors.toMap(i -> columnNames.get(i), i -> values[i].getValue()));
    //        .collect(Collectors.toMap(i -> "Column " + (i + 1), i -> values[i].getValue()));

    return new Node(row.getProbe(), 
        Arrays.asList(geneSymbols), type, new HashMap<String, Double>(weights));
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

  public double weight() {
    return weights.get("Column 1");
  }
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
