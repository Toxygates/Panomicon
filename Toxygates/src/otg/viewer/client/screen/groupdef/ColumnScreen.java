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

import java.util.*;
import java.util.stream.Collectors;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otg.viewer.client.components.*;
import otg.viewer.client.components.compoundsel.CompoundSelector;
import otg.viewer.client.screen.data.DataScreen;
import t.common.shared.Dataset;
import t.common.shared.sample.Group;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.Utils;
import t.viewer.client.future.Future;
import t.viewer.client.future.FutureUtils;
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
  private List<String> chosenCompounds;

  @Override
  public void loadState(AttributeSet attributes) {
    logger.info("columnScreen.loadState");
    List<Dataset> newChosenDatasets = getStorage().datasetsStorage.getIgnoringException();
    SampleClass newSampleClass = getStorage().sampleClassStorage.getIgnoringException();
    
    // Fetch sampleclasses if necessary
    Future<SampleClass[]> sampleClassesFuture = new Future<SampleClass[]>();    
    if (!newChosenDatasets.equals(chosenDatasets)) {
      filterTools.setDatasets(newChosenDatasets);
      fetchSampleClasses(sampleClassesFuture);
    } else {
      logger.info("bypassing sampleclass fetching");
      sampleClassesFuture.onSuccess(null); // TODO better syntax
    }
    
    // After we have sampleclasses, load sampleclass and fetch compounds if necessary
    Future<String[]> compoundsFuture = new Future<String[]>();
    sampleClassesFuture.addSuccessCallback(result -> {
      logger.info("processing sampleclasses");
      //TODO: handle the case where sample class is not valid for dataset choice
      filterTools.sampleClassChanged(newSampleClass);
      
      // We only need to fetch compounds if sample class or datasets have changed
      if (!newSampleClass.equals(chosenSampleClass) || !newChosenDatasets.equals(chosenDatasets)) {
        fetchCompounds(compoundsFuture, newSampleClass);
      } else {
        logger.info("bypassing compounds fetching");
        sampleClassesFuture.onSuccess(null); // TODO better syntax
      }
      chosenDatasets = newChosenDatasets;
      chosenSampleClass = newSampleClass;
    });
    
    compoundsFuture.addSuccessCallback(allCompounds -> {
      logger.info("processing compounds");
      // Filter chosen compounds to valid choices
      chosenCompounds = filterCompounds(getStorage().compoundsStorage.getIgnoringException(), allCompounds);
      getStorage().compoundsStorage.store(chosenCompounds);    
      
      groupInspector.initializeState(chosenDatasets, chosenSampleClass, chosenCompounds);
      groupInspector.loadGroups();
      compoundSelector.setChosenCompounds(chosenCompounds);
    });
  }
  
  public Future<SampleClass[]> fetchSampleClasses(Future<SampleClass[]> future) {
    manager().sampleService().chooseDatasets(chosenDatasets.toArray(new Dataset[0]), future);
    FutureUtils.beginPendingRequestHandling(future, this, "Unable to choose datasets");
    future.addSuccessCallback(sampleClasses -> {
      logger.info("sample classes fetched");
      filterTools.dataFilterEditor.setAvailable(sampleClasses, false);
    });
    return future;
  }
  
  public Future<String[]> fetchCompounds(Future<String[]> future, SampleClass sampleClass) {
    manager().sampleService().parameterValues(sampleClass, schema().majorParameter().id(), future);
    FutureUtils.beginPendingRequestHandling(future, this, "Unable to retrieve values for parameter: ");
    future.addSuccessCallback(compounds -> {
      logger.info("compounds fetched");
      compoundSelector.acceptCompounds(compounds);
    });
    return future;
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
  
  private List<String> filterCompounds(List<String> chosenList, String[] bigList) {
    HashSet<String> compoundsSet = new HashSet<String>(Arrays.asList(bigList));
    return chosenList.stream().filter(c -> compoundsSet.contains(c)).collect(Collectors.toList());
  }

  // FilterTools.Delegate method
  @Override
  public void filterToolsSampleClassChanged(SampleClass newSampleClass) {
    getStorage().sampleClassStorage.store(newSampleClass);
    fetchCompounds(new Future<String[]>(), newSampleClass).addSuccessCallback(allCompounds ->  {
      chosenCompounds = filterCompounds(chosenCompounds, allCompounds);
      getStorage().compoundsStorage.store(chosenCompounds);
      groupInspector.initializeState(chosenDatasets, newSampleClass, chosenCompounds);
    });
    chosenSampleClass = newSampleClass;
  }
  
  @Override
  public void filterToolsDatasetsChanged(List<Dataset> datasets) {
    chosenDatasets = datasets;
    getStorage().datasetsStorage.store(datasets);
    groupInspector.datasetsChanged(datasets);
    //TODO: sampleclass might change here; either that's not being handled or compounds are being
    //fetched twice
    fetchCompounds(new Future<String[]>(), chosenSampleClass);
  }

  // GroupInspector.Delegate methods
  @Override
  public void groupInspectorDatasetsChanged(List<Dataset> datasets) {
    chosenDatasets = datasets;
    filterTools.setDatasets(datasets);
    filterTools.getSampleClasses();
    //same todo as filter tools version
    fetchCompounds(new Future<String[]>(), chosenSampleClass);
  }

  @Override
  public void groupInspectorLoadGroup(Group group, SampleClass sampleClass, List<String> compounds) {
    chosenSampleClass = getStorage().sampleClassStorage.store(sampleClass);
    filterTools.sampleClassChanged(sampleClass);
    //TODO: only fetch compounds when necessary
    fetchCompounds(new Future<String[]>(), sampleClass).addSuccessCallback(allCompounds ->  {
      compoundSelector.acceptCompounds(allCompounds);
      chosenCompounds = filterCompounds(compounds, allCompounds);
      compoundSelector.setChosenCompounds(chosenCompounds);
      getStorage().compoundsStorage.store(chosenCompounds);
      if (chosenCompounds.size() < compounds.size()) {
        Window.alert("chosenCompounds = " + chosenCompounds + "; compounds = " + compounds);
      }
      groupInspector.multiSelectionGrid.setVisibleUnits(group.getUnits());
      groupInspector.initializeState(chosenDatasets, sampleClass, chosenCompounds);
    });
    // also, a dataset change should get worked in here too
    // so enable datasets if needed should instead be a thing that lists any 
    // disabled but necessary datasets. done.
  }
  
  @Override
  public void groupInspectorClearCompounds() {
    compoundSelector.setChosenCompounds(new ArrayList<String>());
    chosenCompounds = getStorage().compoundsStorage.store(new ArrayList<String>());
  }
  
  // CompoundSelector.Delegate methods
  @Override
  public void compoundSelectorItemListsChanged(List<ItemList> itemLists) {
    getStorage().itemListsStorage.store(itemLists);
  }

  @Override
  public void compoundSelectorCompoundsChanged(List<String> compounds) {
    groupInspector.setCompounds(compounds);
    chosenCompounds = getStorage().compoundsStorage.store(compounds);
  }
}
