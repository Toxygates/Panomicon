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

import t.gwt.viewer.client.components.GroupLabels;
import t.gwt.viewer.client.screen.ImportingScreen;
import t.gwt.viewer.client.screen.Screen;
import t.gwt.viewer.client.screen.data.DataScreen;
import t.gwt.viewer.client.screen.data.GeneSetEditor;
import t.gwt.viewer.client.screen.data.GeneSetsMenu;
import t.gwt.viewer.client.screen.groupdef.GroupInspector;
import t.gwt.viewer.client.screen.groupdef.SelectionTDGrid;
import t.gwt.viewer.client.screen.ranking.CompoundRanker;
import t.gwt.common.client.ValueAcceptor;
import t.gwt.common.client.components.StringArrayTable;
import t.shared.common.DataSchema;
import t.shared.viewer.StringList;
import t.shared.viewer.intermine.IntermineInstance;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Factory methods for the different UI types supported. This is not (currently) related to the
 * T/OTG divide, but instead supports constructing different component families for different
 * flavours of the OTG interface.
 */
public interface UIFactory {

  SelectionTDGrid selectionTDGrid(Screen scr);

  CompoundRanker compoundRanker(Screen _screen);
  
  GroupInspector groupInspector(Screen scr,
                                GroupInspector.Delegate delegate);
  
  GroupLabels groupLabels(Screen screen, DataSchema schema, List<ClientGroup> groups);
  
  GeneSetEditor geneSetEditor(ImportingScreen screen);
  
  GeneSetsMenu geneSetsMenu(DataScreen screen);

  /**
   * Perform enrichment for a single gene set.
   * This will display a dialog that lets the user select enrichment parameters before
   * performing the enrichment.
   *   
   * @param screen The screen that displays the enrichment dialog
   * @param list The gene set to enrich (as a probes list)
   * @param preferredInstance The preferred intermine instance that is
   * to compute the enrichment results.
   */
  void displayEnrichmentDialog(ImportingScreen screen, StringList list,
      @Nullable IntermineInstance preferredInstance);

  /**
   * Enrichment for multiple gene sets
   * This will display a dialog that lets the user select enrichment parameters before
   * performing the enrichment.
   * 
   * @param screen The screen that displays the enrichment dialog
   * @param lists The gene sets to enrich (as a probes list)
   * @param preferredInstance The preferred intermine instance that is
   * to compute the enrichment results.
   */
  void displayMultiEnrichmentDialog(ImportingScreen screen, StringList[] lists,
      @Nullable IntermineInstance preferredInstance);

  /**
   * Asynchronously construct a summary of samples for display on the start screen, if appropriate.
   * The resulting summary table will be sent to the provided callback. 
   * The call will not be made if the table should not be displayed.
   */
  default void sampleSummaryTable(Screen screen,
                                  ValueAcceptor<StringArrayTable> acceptor) {    
  }
  
}
