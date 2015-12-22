/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */
package otgviewer.client;

import java.util.List;

import javax.annotation.Nullable;

import otgviewer.client.components.GeneSetEditor;
import otgviewer.client.components.GroupLabels;
import otgviewer.client.components.Screen;
import otgviewer.client.components.compoundsel.CompoundSelector;
import otgviewer.client.components.compoundsel.RankingCompoundSelector;
import otgviewer.client.components.groupdef.GroupInspector;
import otgviewer.client.components.groupdef.SelectionTDGrid;
import otgviewer.client.components.groupdef.SelectionTDGrid.UnitListener;
import otgviewer.client.components.groupdef.TreatedControlGroupInspector;
import otgviewer.client.components.groupdef.TreatedControlSelTDGrid;
import otgviewer.client.components.ranking.CompoundRanker;
import otgviewer.client.components.ranking.SimpleCompoundRanker;
import otgviewer.client.targetmine.TargetMineData;
import t.common.shared.DataSchema;
import t.common.shared.StringList;
import t.common.shared.sample.Group;


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
  public CompoundRanker compoundRanker(Screen _screen, RankingCompoundSelector selector) {
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

  @Override
  public GeneSetsMenuItem geneSetsMenuItem(DataScreen screen) {
    return new GeneSetsMenuItem(screen);
  }

  @Override
  public void enrichment(Screen screen) {
    TargetMineData tm = new TargetMineData(screen);
    tm.enrich();
  }

  @Override
  public void multiEnrichment(Screen screen, StringList[] lists) {
    TargetMineData tm = new TargetMineData(screen);
    tm.multiEnrich(lists);    
  }
  
}
