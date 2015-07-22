package otgviewer.client.components.groupdef;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;

import otgviewer.client.CompoundSelector;
import otgviewer.client.components.Screen;
import otgviewer.shared.Group;

public class TreatedControlGroupInspector extends GroupInspector {

  public TreatedControlGroupInspector(CompoundSelector cs, Screen scr) {
    super(cs, scr);
  }

  protected void makeGroupColumns(CellTable<Group> table) {
    TextColumn<Group> textColumn = new TextColumn<Group>() {
      @Override
      public String getValue(Group object) {
        return "" + object.getTreatedSamples().length;
      }
    };
    table.addColumn(textColumn, "#Treated samples");

    textColumn = new TextColumn<Group>() {
      @Override
      public String getValue(Group object) {
        return "" + object.getControlSamples().length;
      }
    };
    table.addColumn(textColumn, "#Control samples");
  }

}
