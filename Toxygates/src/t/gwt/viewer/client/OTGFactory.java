/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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
package t.gwt.viewer.client;

import java.util.List;

import javax.annotation.Nullable;

import t.gwt.viewer.client.screen.ImportingScreen;
import t.gwt.viewer.client.screen.Screen;
import t.gwt.viewer.client.intermine.InterMineData;
import t.gwt.viewer.client.screen.groupdef.TreatedControlGroupInspector;
import t.gwt.viewer.client.screen.ranking.CompoundRanker;
import t.gwt.viewer.client.screen.ranking.SimpleCompoundRanker;
import t.shared.common.DataSchema;
import t.gwt.viewer.client.components.GroupLabels;
import t.gwt.viewer.client.screen.data.DataScreen;
import t.gwt.viewer.client.screen.data.GeneSetEditor;
import t.gwt.viewer.client.screen.data.GeneSetsMenu;
import t.gwt.viewer.client.screen.groupdef.GroupInspector;
import t.gwt.viewer.client.screen.groupdef.SelectionTDGrid;
import t.gwt.viewer.client.screen.groupdef.TreatedControlSelTDGrid;
import t.shared.viewer.StringList;
import t.shared.viewer.intermine.IntermineInstance;


/**
 * This is the standard factory for new Toxygates/AdjuvantDB instances.
 */
public class OTGFactory implements UIFactory {

  @Override
  public SelectionTDGrid selectionTDGrid(Screen scr) {
    return new TreatedControlSelTDGrid(scr);
  }

  @Override
  public CompoundRanker compoundRanker(Screen _screen) {
    return new SimpleCompoundRanker(_screen);
  }

  @Override
  public GroupInspector groupInspector(Screen scr,
                                       GroupInspector.Delegate delegate) {
    return new TreatedControlGroupInspector(scr, delegate);
  }

  @Override
  public GroupLabels groupLabels(Screen screen, DataSchema schema, List<ClientGroup> groups) {
    return new GroupLabels(screen, schema, groups);
  }

  @Override
  public GeneSetEditor geneSetEditor(ImportingScreen screen) {
    return new GeneSetEditor(screen);
  }  

  @Override
  public GeneSetsMenu geneSetsMenu(DataScreen screen) {
    return new GeneSetsMenu(screen);
  }

  @Override
  public void displayEnrichmentDialog(ImportingScreen screen, StringList probes,
      @Nullable IntermineInstance preferredInst) {
    InterMineData tm = new InterMineData(screen, preferredInst);
    tm.enrich(probes);
  }

  @Override
  public void displayMultiEnrichmentDialog(ImportingScreen screen, StringList[] lists,
      @Nullable IntermineInstance preferredInst) {
    InterMineData tm = new InterMineData(screen, preferredInst);
    tm.multiEnrich(lists);    
  }
}
