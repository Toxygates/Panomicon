package t.viewer.client.table;

import java.util.*;

import t.common.shared.Pair;
import t.common.shared.sample.ExpressionRow;

/**
 * Summarises a single association column as a histogram for a set of rows.
 */
class AssociationSummary {

  class HistogramEntry {
    Pair<String, String> entry;
    int count;
    HistogramEntry(Pair<String, String> entry, int count) {
      this.entry = entry;
      this.count = count;
    }
  }
  
  private List<Pair<String, String>> data = new ArrayList<Pair<String, String>>();
  private Set<Pair<String, String>> unique = new HashSet<Pair<String, String>>();
  private List<HistogramEntry> sortedByCount = new ArrayList<HistogramEntry>();
  
  AssociationSummary(AssociationTable<ExpressionRow>.AssociationColumn col, 
    Collection<ExpressionRow> rows) {
    for (ExpressionRow row: rows) {
      data.addAll(col.getLinkableValues(row));
      unique.addAll(col.getLinkableValues(row));
    }    
    for (Pair<String, String> key: unique) {
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
  
  HistogramEntry countItem(Pair<String, String> item) {
    int count = 0;
    for (Pair<String, String> key: data) {
      if (key.equals(item)) {
        count += 1;
      }
    }
    return new HistogramEntry(item, count);    
  }
  
  String[][] getTable() {
    String[][] r = new String[sortedByCount.size() + 1][3];
    r[0] = new String[] { "Title", "ID", "Count" };
    for (int i = 0; i < sortedByCount.size(); i++) {
      HistogramEntry he = sortedByCount.get(i);
      r[i + 1][0] = he.entry.first();
      r[i + 1][1] = he.entry.second();
      r[i + 1][2] = he.count + "";
    }
    return r;
  }
  
}
