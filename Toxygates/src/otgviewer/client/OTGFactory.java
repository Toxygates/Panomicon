package otgviewer.client;

import javax.annotation.Nullable;

import otgviewer.client.components.Screen;
import otgviewer.client.components.groupdef.SelectionTDGrid;
import otgviewer.client.components.groupdef.TreatedControlSelTDGrid;
import otgviewer.client.components.groupdef.SelectionTDGrid.UnitListener;

public class OTGFactory implements UIFactory {

  @Override
  public SelectionTDGrid selectionTDGrid(Screen scr, @Nullable UnitListener listener) {
    return new TreatedControlSelTDGrid(scr, listener);
  }
  
  @Override
  public CompoundSelector compoundSelector(Screen screen, String heading) {
    return new CompoundSelector(screen, heading, true, true);
  }

}
