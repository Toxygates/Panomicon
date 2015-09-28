package otgviewer.client;

import java.util.List;

import javax.annotation.Nullable;

import otgviewer.client.components.GeneSetEditor;
import otgviewer.client.components.GroupLabels;
import otgviewer.client.components.Screen;
import otgviewer.client.components.groupdef.GroupInspector;
import otgviewer.client.components.groupdef.SelectionTDGrid;
import otgviewer.client.components.groupdef.SelectionTDGrid.UnitListener;
import otgviewer.client.components.ranking.CompoundRanker;
import otgviewer.shared.Group;
import t.common.shared.DataSchema;

public interface UIFactory {

  public SelectionTDGrid selectionTDGrid(Screen scr, @Nullable UnitListener listener);

  public CompoundSelector compoundSelector(Screen screen, String heading);
  
  public CompoundRanker compoundRanker(Screen _screen, CompoundSelector selector);
  
  public GroupInspector groupInspector(CompoundSelector cs, Screen scr);
  
  public GroupLabels groupLabels(Screen screen, DataSchema schema, List<Group> groups);
  
  public GeneSetEditor geneSetEditor(Screen screen);
  
  public boolean hasHeatMapMenu();
}
