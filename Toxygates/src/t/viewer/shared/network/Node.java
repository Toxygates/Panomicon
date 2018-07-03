package t.viewer.shared.network;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import t.common.shared.SharedUtils;
import t.common.shared.sample.ExpressionRow;

@SuppressWarnings("serial")
public class Node implements Serializable {
  String id, type;
  List<String> symbols;
  double weight;
  
  //GWT constructor
  Node() {}
  

  public static Node fromRow(ExpressionRow row, String type) {
    // When there's no gene symbol, for some reason row.geneSymbols gives us a singleton
    // list with an empty string, which we don't want here.
    String[] geneSymbols = row.getGeneSyms();
    if (geneSymbols.length == 1 && geneSymbols[0] == "") {
      geneSymbols = new String[0];
    }
    return new Node(row.getProbe(), 
        Arrays.asList(geneSymbols), type, row.getValue(0).getValue());
  }
  
  public Node(String id, List<String> symbols, String type, double weight) {
    this.id = id;
    this.symbols = symbols;
    this.type = type;
    this.weight = weight;
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
    return weight;
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
