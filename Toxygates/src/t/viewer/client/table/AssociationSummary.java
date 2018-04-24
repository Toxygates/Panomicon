package t.viewer.client.table;

import java.util.*;
import java.util.stream.Collectors;

import t.common.shared.sample.ExpressionRow;
import t.viewer.shared.AssociationValue;

/**
 * Summarises a single association column as a histogram for a set of rows.
 */
public class AssociationSummary<T extends ExpressionRow> {

  class HistogramEntry {
    AssociationValue entry;

    int count;
    HistogramEntry(AssociationValue entry, int count) {
      this.entry = entry;
      this.count = count;
    }
  }
  
  private List<AssociationValue> data = new ArrayList<AssociationValue>();
  private Set<AssociationValue> unique = new HashSet<AssociationValue>();
  private List<HistogramEntry> sortedByCount = new ArrayList<HistogramEntry>();
  private Map<String, Collection<String>> full = 
      new HashMap<String, Collection<String>>();
  private Map<String, Collection<String>> reverseMap;
  
  AssociationSummary(AssociationTable<T>.AssociationColumn col, 
    Collection<T> rows) {
    for (T row: rows) {
      Collection<AssociationValue> values = col.getLinkableValues(row);
      data.addAll(values);
      unique.addAll(values);
      full.put(row.getProbe(), values.stream().map(v ->
        v.formalIdentifier()).collect(Collectors.toList()));
    }    
    for (AssociationValue key: unique) {
      HistogramEntry he = countItem(key);
      sortedByCount.add(he);
    }
    Collections.sort(sortedByCount, new Comparator<HistogramEntry>() {

      //Sort in descending order by count
      @Override
      public int compare(HistogramEntry o1, HistogramEntry o2) {
        return o2.count - o1.count;
      }
      
    });
  }
  
  HistogramEntry countItem(AssociationValue item) {
    int count = 0;
    for (AssociationValue key: data) {
      if (key.equals(item)) {
        count += 1;
      }
    }
    return new HistogramEntry(item, count);    
  }
  
  /**
   * Histogram summary table.
   * @return
   */
  public String[][] getTable() {
    String[][] r = new String[sortedByCount.size() + 1][3];
    r[0] = new String[] { "Title", "ID", "Count" };
    for (int i = 0; i < sortedByCount.size(); i++) {
      HistogramEntry he = sortedByCount.get(i);
      r[i + 1][0] = he.entry.title();
      r[i + 1][1] = he.entry.formalIdentifier();
      r[i + 1][2] = he.count + "";
    }
    return r;
  }
  
  /**
   * Maps each row to all association values for that row.
   */
  public Map<String, Collection<String>> getFullMap() { return full; }
  
  public Map<String, Collection<String>> getReverseMap() {
    if (reverseMap == null) {
      reverseMap = new HashMap<String, Collection<String>>();
      for (String k : full.keySet()) {
        for (String id: full.get(k)) {          
          if (!reverseMap.containsKey(id)) {
            reverseMap.put(id, new LinkedList<String>());
          }
          reverseMap.get(id).add(k);
        }
      }
    }
    return reverseMap;    
  }
  
}
