package t.viewer.client.table;

import java.util.*;

import t.common.shared.sample.ExpressionRow;
import t.viewer.shared.AssociationValue;

/**
 * Summarises a single association column as a histogram for a set of rows.
 */
class AssociationSummary {

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
  
  AssociationSummary(AssociationTable<ExpressionRow>.AssociationColumn col, 
    Collection<ExpressionRow> rows) {
    for (ExpressionRow row: rows) {
      data.addAll(col.getLinkableValues(row));
      unique.addAll(col.getLinkableValues(row));
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
  
  String[][] getTable() {
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
  
}
