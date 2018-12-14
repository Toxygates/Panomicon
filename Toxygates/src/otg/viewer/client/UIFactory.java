/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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
package otg.viewer.client;

import java.util.List;

import javax.annotation.Nullable;

import otg.viewer.client.components.*;
import otg.viewer.client.components.compoundsel.CompoundSelector;
import otg.viewer.client.screen.data.*;
import otg.viewer.client.screen.groupdef.GroupInspector;
import otg.viewer.client.screen.groupdef.SelectionTDGrid;
import otg.viewer.client.screen.groupdef.SelectionTDGrid.UnitListener;
import otg.viewer.client.screen.ranking.CompoundRanker;
import t.common.client.ValueAcceptor;
import t.common.client.components.StringArrayTable;
import t.common.shared.DataSchema;
import t.common.shared.sample.Group;
import t.viewer.shared.StringList;
import t.viewer.shared.intermine.IntermineInstance;

/**
 * Factory methods for the different UI types supported. This is not (currently) related to the
 * T/OTG divide, but instead supports constructing different component families for different
 * flavours of the OTG interface.
 */
public interface UIFactory {

  public SelectionTDGrid selectionTDGrid(OTGScreen scr, @Nullable UnitListener listener);

  public CompoundRanker compoundRanker(OTGScreen _screen);
  
  public GroupInspector groupInspector(CompoundSelector cs, OTGScreen scr,
      GroupInspector.Delegate delegate);
  
  public GroupLabels groupLabels(OTGScreen screen, DataSchema schema, List<Group> groups);
  
  public GeneSetEditor geneSetEditor(ImportingScreen screen);
  
  public boolean hasHeatMapMenu();
  
  public GeneSetsMenu geneSetsMenu(DataScreen screen);

  /**
   * Enrichment for a gene set
   */
  void enrichment(ImportingScreen screen, StringList list,
      @Nullable IntermineInstance preferredInstance);

  /**
   * Enrichment for multiple gene sets
   */
  void multiEnrichment(ImportingScreen screen, StringList[] lists,
      @Nullable IntermineInstance preferredInstance);

  /**
   * Call back with a summary of samples for display on the start screen, if appropriate. The call
   * will not be made if the table should not be displayed.
   */
  default void sampleSummaryTable(OTGScreen screen, 
                                  ValueAcceptor<StringArrayTable> acceptor) {    
  }
  
}
