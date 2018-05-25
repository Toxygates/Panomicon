/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.*;
import otgviewer.client.components.compoundsel.CompoundSelector;
import otgviewer.client.components.groupdef.GroupInspector;
import t.common.shared.Dataset;
import t.common.shared.ItemList;
import t.common.shared.sample.Group;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.Utils;

/**
 * This screen allows for column (group) definition as well as compound ranking.
 */
public class ColumnScreen extends MinimalScreen implements FilterTools.Delegate,
    GroupInspector.Delegate, CompoundSelector.Delegate {
  public static String key = "columns";

  private GroupInspector groupInspector;
  private CompoundSelector compoundSelector;
  private FilterTools filterTools;

  protected Dataset[] chosenDatasets;
  protected List<Group> chosenColumns = new ArrayList<Group>();

  @Override
  public void loadState(AttributeSet attributes) {
    chosenDatasets = getParser().getDatasets();
    filterTools.datasetsChanged(chosenDatasets);
    SampleClass sampleClass = getParser().getSampleClass(attributes);
    filterTools.sampleClassChanged(sampleClass);
    compoundSelector.sampleClassChanged(sampleClass);
    chosenColumns = getParser().getChosenColumns(schema(), attributes);
    List<String> compounds = getParser().getCompounds();
    groupInspector.compoundsChanged(compounds);
    groupInspector.columnsChanged(chosenColumns);

    if (visible) {
      try {
        List<Group> ics =
            getParser().getColumns(schema(), "inactiveColumns", attributes());
        // loadColumns(getParser(), schema(), "inactiveColumns", new
        // ArrayList<SampleColumn>(groupInspector
        // .existingGroupsTable().inverseSelection()), attributes());
        if (ics != null && ics.size() > 0) {
          logger.info("Unpacked i. columns: " + ics.get(0) + ": " + ics.get(0).getSamples()[0]
              + " ... ");
          groupInspector.inactiveColumnsChanged(ics);
        } else {
          logger.info("No inactive columns available");
        }

      } catch (Exception e) {
        logger.log(Level.WARNING, "Unable to load inactive columns", e);
        Window.alert("Unable to load inactive columns.");
      }
    }

    // This needs to happen after groupInspector.inactiveColumnsChanged, which causes
    // the compound selector's selection to be cleared.
    compoundSelector.compoundsChanged(compounds);
  }

  public ColumnScreen(ScreenManager man) {
    super("Sample groups", key, man, man.resources().groupDefinitionHTML(),
        man.resources().groupDefinitionHelp());

    compoundSelector = new CompoundSelector(this, man.schema().majorParameter().title(), true, true) {
      @Override
      public void changeCompounds(List<String> compounds) {
        super.changeCompounds(compounds);
        groupInspector.compoundsChanged(compounds);
        ColumnScreen.this.getParser().storeCompounds(compounds);
      }

      @Override
      public void sampleClassChanged(SampleClass sc) {
        super.sampleClassChanged(sc);
        groupInspector.sampleClassChanged(sc);
      }
    };
    compoundSelector.addStyleName("compoundSelector");

    chosenDatasets = appInfo().datasets();
    filterTools = new FilterTools(this);
  }

  @Override
  protected void addToolbars() {   
    super.addToolbars();   
    HorizontalPanel hp = Utils.mkHorizontalPanel(false, filterTools);
    addToolbar(hp, 0);
    addLeftbar(compoundSelector, 350);
  }

  @Override
  protected boolean shouldShowStatusBar() {
    return false;
  }

  @Override
  public Widget content() {
    groupInspector = factory().groupInspector(compoundSelector, this, this);
    groupInspector.datasetsChanged(chosenDatasets);
    groupInspector.addStaticGroups(appInfo().predefinedSampleGroups());
    return groupInspector;
  }

  @Override
  public Widget bottomContent() {
    HorizontalPanel hp = Utils.mkWidePanel();

    Button b = new Button("Delete all groups",
        (ClickHandler) e -> groupInspector.confirmDeleteAllGroups());     

    Button b2 = new Button("Next: View data", 
        (ClickHandler) e -> {
        if (groupInspector.chosenColumns().size() == 0) {
          Window.alert("Please define and activate at least one group.");
        } else {
          configuredProceed(DataScreen.key);
        }
      });

    hp.add(Utils.mkHorizontalPanel(true, b, b2));
    return hp;
  }

  @Override
  public void tryConfigure() {
    if (chosenColumns != null && chosenColumns.size() > 0) {
      setConfigured(true);
    }
  }

  @Override
  public void resizeInterface() {
    // Test carefully in IE8, IE9 and all other browsers if changing this method
    compoundSelector.resizeInterface();
    super.resizeInterface();
  }

  @Override
  public String getGuideText() {
    return "Please define at least one sample group to proceed. Start by selecting compounds to the left. Then select doses and times.";
  }

  // FilterTools.Delegate method
  @Override
  public void filterToolsSampleClassChanged(SampleClass sc) {
    getParser().storeSampleClass(sc);
    compoundSelector.sampleClassChanged(sc);
    groupInspector.sampleClassChanged(sc);
  }

  // GroupInspector.Delegate methods
  @Override
  public void groupInspectorDatasetsChanged(Dataset[] ds) {
    chosenDatasets = ds;
    filterTools.datasetsChanged(ds);
  }

  @Override
  public void groupInspectorSampleClassChanged(SampleClass sc) {
    filterTools.sampleClassChanged(sc);
  }

  // CompoundSelector.Delegate methods
  @Override
  public void CompoundSelectorItemListsChanged(List<ItemList> itemLists) {
    getParser().storeItemLists(itemLists);
  }
}
