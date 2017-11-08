package t.viewer.client.table;

import otgviewer.client.StandardColumns;

/**
 * Controls the overall style of an ExpressionTable.
 */
public abstract class TableStyle {
  abstract boolean initVisibility(StandardColumns col);
  abstract String initWidth(StandardColumns col);
  
  public static TableStyle getStyle(String name) {
    if (name.equals("mirna")) {
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
