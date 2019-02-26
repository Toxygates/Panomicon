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

package otg.viewer.client.screen.groupdef;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otg.viewer.client.components.*;
import otg.viewer.client.components.compoundsel.CompoundSelector;
import otg.viewer.client.screen.data.DataScreen;
import t.common.shared.Dataset;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.Utils;
import t.viewer.shared.ItemList;

/**
 * This screen allows for column (group) definition as well as compound ranking.
 */
public class ColumnScreen extends MinimalScreen implements FilterTools.Delegate,
    GroupInspector.Delegate, CompoundSelector.Delegate {
  public final static String key = "columns";

  private GroupInspector groupInspector;
  private CompoundSelector compoundSelector;
  private FilterTools filterTools;

  protected List<Dataset> chosenDatasets = new ArrayList<Dataset>();
  private SampleClass chosenSampleClass;

  @Override
  public void loadState(AttributeSet attributes) {
    List<Dataset> newChosenDatasets = getStorage().datasetsStorage.getIgnoringException();
    filterTools.datasetsChanged(newChosenDatasets);
    groupInspector.datasetsChanged(newChosenDatasets);

    SampleClass newSampleClass = getStorage().sampleClassStorage.getIgnoringException();
    filterTools.sampleClassChanged(newSampleClass);
    groupInspector.sampleClassChanged(newSampleClass);
    compoundSelector.sampleClassChanged(newSampleClass);
    
    if (!newSampleClass.equals(chosenSampleClass) || !newChosenDatasets.equals(chosenDatasets)) {
      compoundSelector.fetchCompounds();
    }
    chosenDatasets = newChosenDatasets;
    chosenSampleClass = newSampleClass;

    List<String> chosenCompounds = getStorage().compoundsStorage.getIgnoringException();
    groupInspector.setCompounds(chosenCompounds);
    groupInspector.loadGroups();
    // This needs to happen after groupInspector.loadGroups, which causes
    // the compound selector's selection to be cleared.
    compoundSelector.setChosenCompounds(chosenCompounds);
  }

  public ColumnScreen(ScreenManager man) {
    super("Sample groups", key, man, man.resources().groupDefinitionHTML(),
        man.resources().groupDefinitionHelp());

    compoundSelector = new CompoundSelector(this, man.schema().majorParameter().title(), true, true);
    compoundSelector.addStyleName("compoundSelector");
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
  protected Widget content() {
    groupInspector = factory().groupInspector(this, this);
    groupInspector.datasetsChanged(chosenDatasets);
    return groupInspector;
  }

  @Override
  protected Widget bottomContent() {
    HorizontalPanel hp = Utils.mkWidePanel();

    Button b = new Button("Delete all groups",
        (ClickHandler) e -> groupInspector.confirmDeleteAllGroups());     

    Button b2 = new Button("Next: View data", 
        (ClickHandler) e -> {
        if (groupInspector.groups.size() == 0) {
          Window.alert("Please define and activate at least one group.");
        } else {
            manager.attemptProceed(DataScreen.key);
        }
      });

    hp.add(Utils.mkHorizontalPanel(true, b, b2));
    return hp;
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
  public void filterToolsSampleClassChanged(SampleClass newSampleClass) {
    getStorage().sampleClassStorage.store(newSampleClass);
    compoundSelector.sampleClassChanged(newSampleClass);
    groupInspector.sampleClassChanged(newSampleClass);
    compoundSelector.fetchCompounds();
    chosenSampleClass = newSampleClass;
  }
  
  @Override
  public void filterToolsDatasetsChanged(List<Dataset> datasets) {
    getStorage().datasetsStorage.store(datasets);
    groupInspector.datasetsChanged(datasets);
    compoundSelector.fetchCompounds();
  }

  // GroupInspector.Delegate methods
  @Override
  public void groupInspectorDatasetsChanged(List<Dataset> datasets) {
    chosenDatasets = datasets;
    filterTools.datasetsChanged(datasets);
    compoundSelector.fetchCompounds();
  }

  @Override
  public void groupInspectorSampleClassChanged(SampleClass newSampleClass) {
    getStorage().sampleClassStorage.store(newSampleClass);
    filterTools.sampleClassChanged(newSampleClass);
    compoundSelector.sampleClassChanged(newSampleClass);
    compoundSelector.fetchCompounds();
    chosenSampleClass = newSampleClass;
  }
  
  @Override
  public void groupInspectorCompoundsChanged(List<String> compounds) {
    compoundSelector.setChosenCompounds(compounds);
    getStorage().compoundsStorage.store(compounds);
  }
  
  // CompoundSelector.Delegate methods
  @Override
  public void compoundSelectorItemListsChanged(List<ItemList> itemLists) {
    getStorage().itemListsStorage.store(itemLists);
  }

  @Override
  public void compoundSelectorCompoundsChanged(List<String> compounds) {
    groupInspector.setCompounds(compounds);
    getStorage().compoundsStorage.store(compounds);
  }
}
