package otgviewer.client;

import java.util.List;

import otgviewer.client.components.GeneSetEditor;
import otgviewer.client.components.GroupLabels;
import otgviewer.client.components.Screen;
import otgviewer.client.components.groupdef.GroupInspector;
import otgviewer.client.components.groupdef.SelectionTDGrid;
import otgviewer.client.components.groupdef.SelectionTDGrid.UnitListener;
import otgviewer.client.components.groupdef.TreatedControlGroupInspector;
import otgviewer.client.components.groupdef.TreatedControlSelTDGrid;
import otgviewer.client.components.ranking.CompoundRanker;
import otgviewer.client.components.ranking.SimpleCompoundRanker;
import otgviewer.shared.Group;
import t.common.shared.DataSchema;

import com.sun.istack.internal.Nullable;

/**
 * This is the standard factory for new Toxygates/AdjuvantDB instances.
 */
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

  @Override
  public GroupInspector groupInspector(CompoundSelector cs, Screen scr) {
    return new TreatedControlGroupInspector(cs, scr);
  }

  @Override
  public GroupLabels groupLabels(Screen screen, DataSchema schema, List<Group> groups) {
    return new GroupLabels(screen, schema, groups);
  }

  @Override
  public GeneSetEditor geneSetEditor(Screen screen) {
    return new GeneSetEditor(screen);
  }  
  
  @Override
  public boolean hasHeatMapMenu() {
    return true;
  }
}
