package otgviewer.client;

import javax.annotation.Nullable;

import otgviewer.client.components.Screen;
import otgviewer.client.components.groupdef.SelectionTDGrid;
import otgviewer.client.components.groupdef.SelectionTDGrid.UnitListener;

public interface UIFactory {

  public SelectionTDGrid selectionTDGrid(Screen scr, @Nullable UnitListener listener);

  public CompoundSelector compoundSelector(Screen screen, String heading);
}
