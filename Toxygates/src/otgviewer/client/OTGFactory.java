package otgviewer.client;

import javax.annotation.Nullable;

import otgviewer.client.components.Screen;
import otgviewer.client.components.groupdef.SelectionTDGrid;
import otgviewer.client.components.groupdef.TreatedControlSelTDGrid;
import otgviewer.client.components.groupdef.SelectionTDGrid.UnitListener;
import otgviewer.client.components.ranking.CompoundRanker;
import otgviewer.client.components.ranking.SimpleCompoundRanker;

public class OTGFactory implements UIFactory {

  @Override
  public SelectionTDGrid selectionTDGrid(Screen scr, @Nullable UnitListener listener) {
    return new TreatedControlSelTDGrid(scr, listener);
  }
  
  @Override
  public CompoundSelector compoundSelector(Screen screen, String heading) {
    return new CompoundSelector(screen, heading, true, true);
  }

  @Override
  public CompoundRanker compoundRanker(Screen _screen, CompoundSelector selector) {
    return new SimpleCompoundRanker(_screen, selector);
  }
}
