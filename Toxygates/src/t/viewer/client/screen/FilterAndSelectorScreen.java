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

package t.viewer.client.screen;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Window;
import t.common.shared.Dataset;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.components.compoundsel.CompoundSelector;
import t.viewer.client.future.Future;
import t.viewer.client.future.FutureUtils;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains functionality for managing the interaction between FilterTools and 
 * a CompoundSelector, necessary in ColumnScreen and RankingScreen.
 */
public abstract class FilterAndSelectorScreen extends FilterScreen {
  protected CompoundSelector compoundSelector;
  
  protected List<Dataset> chosenDatasets;
  protected SampleClass chosenSampleClass;
  protected List<String> chosenCompounds;
  
  protected FilterAndSelectorScreen(String title, String key, ScreenManager man,
      @Nullable TextResource helpHTML, @Nullable ImageResource helpImage) {
    super(title, key, man, helpHTML, helpImage);
  }

  /**
   * Loads chosen datasets and chosen sampleclass from local storage, then
   * 1) sets datasets on the server and retrieve sampleclasses if necessary, and
   * 2) fetches compounds based on the chosen datasets and sampleclass, if necessary.
   * @param attributes
   * @return a future that completes with compounds, if fetched
   */
  public Future<String[]> loadDatasetsAndSampleClass(AttributeSet attributes) {
    List<Dataset> newChosenDatasets = getStorage().datasetsStorage.getIgnoringException();
    SampleClass newSampleClass = getStorage().sampleClassStorage.getIgnoringException();
    
    Future<SampleClass[]> sampleClassesFuture = new Future<SampleClass[]>();
    Future<String[]> compoundsFuture = new Future<String[]>();
    
    // If chosen datasets have changed, process them and also fetch sampleclasses
    if (!newChosenDatasets.equals(chosenDatasets)) {
      filterTools.setDatasets(newChosenDatasets);
      chosenDatasets = newChosenDatasets;
      fetchSampleClasses(sampleClassesFuture, newChosenDatasets);
    } else {
      sampleClassesFuture.bypass();
    }
    
    // After we have sampleclasses, load sampleclass, fetch compounds, and process them
    // if necessary
    processSampleClassesLater(sampleClassesFuture, compoundsFuture, newSampleClass,
        !newSampleClass.equals(chosenSampleClass), getStorage().compoundsStorage.getIgnoringException());
    chosenSampleClass = newSampleClass;
    
    return compoundsFuture;
  }
  
  /**
   * Adds a callback to a Future<SampleClass[]> that makes sure that the chosen sample
   * class is included in the result, displaying a warning otherwise.
   * @param sampleClassesFuture callback will be added to this future.
   */
  protected void warnLaterIfSampleClassInvalid(Future<SampleClass[]> sampleClassesFuture) {
    sampleClassesFuture.addNonErrorCallback(f -> {
      if (!filterTools.dataFilterEditor.availableSampleClasses().stream().anyMatch(chosenSampleClass::equals)) {
        Window.alert("Tried to pick a sampleclass, " + chosenSampleClass + 
            " that is not valid for the current choice of datasets. This could be "  
            + "due to changes in backend data. Application may now be in an "
            + "inconsistent state.");
      }
    });
  }
  
  /**
   * Adds a callback to a Future<SampleClass[]> that does some processing, and then
   * fetch compounds using a provided compoundsFuture if necessary.
   * @param sampleClassesFuture Caller should ensure that this future will complete
   * with sampleclasses; this method will add a callback to this future.
   * @param compoundsFuture compounds will be fetched with this future
   * @param sampleClass the sample class to be treated as the chosen sampleclass
   * @param sampleClassChanged whether chosen sample class has changed
   * @param newChosenCompounds the new chosen compounds that should be selected
   */
  protected void processSampleClassesLater(Future<SampleClass[]> sampleClassesFuture, 
      Future<String[]> compoundsFuture,  SampleClass sampleClass, 
      boolean sampleClassChanged, List<String> newChosenCompounds) {
    sampleClassesFuture.addNonErrorCallback(f -> {
      // Ensure that we have a valid sampleclass by trying to pick it in filterTools,
      // then reading the closest match off of the filterTools.
      filterTools.setSampleClass(sampleClass);
      chosenSampleClass = getStorage().sampleClassStorage
          .store(filterTools.dataFilterEditor.currentSampleClassShowing());
      
      // We only need to fetch compounds if sample class or datasets have changed
      if (sampleClassesFuture.doneAndSuccessful() || sampleClassChanged) {
        fetchCompounds(compoundsFuture, chosenSampleClass);
      } else {
        compoundsFuture.bypass();
      }
    });
    filterChosenCompoundsLater(compoundsFuture, newChosenCompounds);
  }
  
  /**
   * Add callbacks to a Future<String[]> (presumably one that fetched and processed compounds)
   * to filter chosen compounds on the current set of available compounds, and process the new
   * result. 
   * 
   * @param compoundsFuture Caller should ensure that this future will complete compounds; 
   * this method will add callbacks to this future.
   * @param newChosenCompounds A new set of compounds to be selected in the compound selector
   */
  protected void filterChosenCompoundsLater(Future<String[]> compoundsFuture, 
      List<String> newChosenCompounds) {
    compoundsFuture.addSuccessCallback(compounds -> {
      compoundSelector.acceptCompounds(compounds);
    });
    compoundsFuture.addNonErrorCallback(f -> {
      chosenCompounds = filterCompounds(newChosenCompounds, compoundSelector.allCompounds());
      getStorage().compoundsStorage.store(chosenCompounds);    
      compoundSelector.setChosenCompounds(chosenCompounds);
    });
  }
  
  /**
   * Filters a list of chosen compounds based on membership in the larger list of valid compounds 
   * @param chosenList the list of compounds to filter
   * @param bigList compounds not contained in this list will be filtered out
   * @return a list containing all members of chosenList that are also contained in bigList
   */
  private List<String> filterCompounds(List<String> chosenList, List<String> bigList) {
    HashSet<String> compoundsSet = new HashSet<String>(bigList);
    return chosenList.stream().filter(c -> compoundsSet.contains(c)).collect(Collectors.toList());
  }
  
  /**
   * Makes an RPC call to the server to fetch the set of compounds valid for a given sample class
   * @param future this future will complete with the result of the RPC call
   * @param sampleClass compounds valid for this sample class will be fetched 
   * @return the same future that was passed in
   */
  public Future<String[]> fetchCompounds(Future<String[]> future, SampleClass sampleClass) {
    manager().sampleService().parameterValues(sampleClass, schema().majorParameter().id(), future);
    FutureUtils.beginPendingRequestHandling(future, this, "Unable to retrieve values for parameter: ");
    return future;
  }

  /**
   * Update the sample class for this screen, store it in local storage, and fetch 
   * the set of compounds valid for it.
   * @param newSampleClass the sample class to switch to
   * @return a future that completes with  fetched compounds
   */
  public Future<String[]> setSampleClassAndFetchCompounds(SampleClass newSampleClass) {
    getStorage().sampleClassStorage.store(newSampleClass);
    Future<String[]> future = new Future<String[]>();
    fetchCompounds(future, newSampleClass);
    filterChosenCompoundsLater(future, chosenCompounds);
    chosenSampleClass = newSampleClass;
    return future;
  }
  
  // FilterTools.Delegate methods
  /**
   * Called by FilterTools when the user changes the current sample class using the
   * data filter editor.
   * @param newSampleClass the sample class switched to in the data filter editor  
   */
  public void filterToolsSampleClassChanged(SampleClass newSampleClass) {
    setSampleClassAndFetchCompounds(newSampleClass);
  }
  
  /**
   * Called by FilterTools when the user changes the chosen datasets. FilterTools
   * is responsible for creating sampleClassesFuture and making the RPC call to
   * retrieve sample classes using it.
   * Stores the new set of chosen datasets to local storage, and adds a callback
   * to the sample class that 1) fetches future to update the chosen sample class if
   * necessary, and 2) fetches compounds for the updated datasets and sample class.
   * @param datasets the new set of datasets selected in FilterTools
   * @param sampleClassesFuture A future that will complete with the sampleclasses
   * valid for the new chosen datasets. 
   * @return
   */
  public Future<?> filterToolsDatasetsChanged(List<Dataset> datasets) {
    Future<SampleClass[]> sampleClassesFuture = fetchSampleClasses(new Future<SampleClass[]>(), datasets);
    chosenDatasets = getStorage().datasetsStorage.store(datasets);
    Future<String[]> compoundsFuture = new Future<String[]>();
    sampleClassesFuture.addSuccessCallback(sampleClasses -> {
      chosenSampleClass = getStorage().sampleClassStorage
          .store(filterTools.dataFilterEditor.currentSampleClassShowing());
      fetchCompounds(compoundsFuture, chosenSampleClass);
    });
    filterChosenCompoundsLater(compoundsFuture, chosenCompounds);
    return compoundsFuture;
  }
  
  // CompoundSelector.Delegate methods
  /**
   * Called by compound selector when the user edits the set of chosen compounds
   * @param compounds the new set of chosen compounds
   */
  public void compoundSelectorCompoundsChanged(List<String> compounds) {
    chosenCompounds = getStorage().compoundsStorage.store(compounds);
  }
}
