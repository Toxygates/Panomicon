package otg.viewer.client.components;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Window;

import otg.viewer.client.components.compoundsel.CompoundSelector;
import t.common.shared.Dataset;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.future.Future;
import t.viewer.client.future.FutureUtils;
import t.viewer.shared.ItemList;

public abstract class FilterAndSelectorScreen extends FilterScreen {
  protected CompoundSelector compoundSelector;
  
  protected List<Dataset> chosenDatasets = new ArrayList<Dataset>();
  protected SampleClass chosenSampleClass;
  protected List<String> chosenCompounds;
  
  protected FilterAndSelectorScreen(String title, String key, ScreenManager man,
      @Nullable TextResource helpHTML, @Nullable ImageResource helpImage) {
    super(title, key, man, helpHTML, helpImage);
  }

  public Future<String[]> loadDatasetsAndSampleClass(AttributeSet attributes) {
    List<Dataset> newChosenDatasets = getStorage().datasetsStorage.getIgnoringException();
    SampleClass newSampleClass = getStorage().sampleClassStorage.getIgnoringException();
    
    Future<SampleClass[]> sampleClassesFuture = new Future<SampleClass[]>();
    Future<String[]> compoundsFuture = new Future<String[]>();
    
    // Fetch sampleclasses if necessary
    if (!newChosenDatasets.equals(chosenDatasets)) {
      filterTools.setDatasets(newChosenDatasets);
      chosenDatasets = newChosenDatasets;
      fetchSampleClasses(sampleClassesFuture, newChosenDatasets);
    } else {
      sampleClassesFuture.bypass();
    }
    
    // After we have sampleclasses, load sampleclass and fetch compounds if necessary
    warnIfSampleClassInvalid(sampleClassesFuture);
    processSampleClasses(sampleClassesFuture, compoundsFuture, newSampleClass,
        !newSampleClass.equals(chosenSampleClass));
    chosenSampleClass = newSampleClass;
    
    processCompounds(compoundsFuture, getStorage().compoundsStorage.getIgnoringException());
    return compoundsFuture;
  }
  
  protected void processSampleClasses(Future<SampleClass[]> sampleClassesFuture, Future<String[]> compoundsFuture, 
      SampleClass sampleClass, boolean foo) {
    sampleClassesFuture.addNonErrorCallback(() -> {
      logger.info("processing sampleclasses");
      filterTools.setSampleClass(sampleClass);
      
      // We only need to fetch compounds if sample class or datasets have changed
      if (sampleClassesFuture.actuallyRan() || foo) {
        fetchCompounds(compoundsFuture, sampleClass);
      } else {
        compoundsFuture.bypass();
      }
    });
  }
  
  protected void processCompounds(Future<String[]> compoundsFuture, 
      List<String> newChosenCompounds) {
    compoundsFuture.addSuccessCallback(allCompounds ->  {
      compoundSelector.acceptCompounds(allCompounds);
    });
    compoundsFuture.addNonErrorCallback(() -> {
      chosenCompounds = filterCompounds(newChosenCompounds, compoundSelector.allCompounds());
      logger.info("filtered compounds = " + chosenCompounds);
      getStorage().compoundsStorage.store(chosenCompounds);    
      compoundSelector.setChosenCompounds(chosenCompounds);
    });
  }
  
  private List<String> filterCompounds(List<String> chosenList, List<String> bigList) {
    HashSet<String> compoundsSet = new HashSet<String>(bigList);
    return chosenList.stream().filter(c -> compoundsSet.contains(c)).collect(Collectors.toList());
  }
  
  protected void warnIfSampleClassInvalid(Future<SampleClass[]> sampleClassesFuture) {
    sampleClassesFuture.addSuccessCallback(sampleClasses -> {
      if (!Arrays.stream(sampleClasses).anyMatch(chosenSampleClass::equals)) {
        Window.alert("Tried to pick a sampleclass, " + chosenSampleClass + 
            " that is not valid for te current choice of datasets. This could be "  
            + "due to changes in backend data; Application may now be in an "
            + "inconsistent state.");
      }
    });
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

  // FilterTools.Delegate methods
  public void filterToolsSampleClassChanged(SampleClass newSampleClass) {
    setSampleClassAndFetchCompounds(newSampleClass);
  }
  
  public Future<String[]> setSampleClassAndFetchCompounds(SampleClass newSampleClass) {
    getStorage().sampleClassStorage.store(newSampleClass);
    Future<String[]> future = new Future<String[]>();
    fetchCompounds(future, newSampleClass);
    processCompounds(future, chosenCompounds);
    chosenSampleClass = newSampleClass;
    return future;
  }
  
  public Future<?> filterToolsDatasetsChanged(List<Dataset> datasets, 
      Future<SampleClass[]> sampleClassesFuture) {
    chosenDatasets = getStorage().datasetsStorage.store(datasets);
    Future<String[]> compoundsFuture = new Future<String[]>();
    processCompounds(compoundsFuture, chosenCompounds);
    sampleClassesFuture.addSuccessCallback(sampleClasses -> {
      chosenSampleClass = getStorage().sampleClassStorage
          .store(filterTools.dataFilterEditor.currentSampleClassShowing());
      fetchCompounds(compoundsFuture, chosenSampleClass);
      logger.info("just processed some compounds");
    });
    return compoundsFuture;
  }
  
  // CompoundSelector.Delegate methods
  public void compoundSelectorItemListsChanged(List<ItemList> itemLists) {
    getStorage().itemListsStorage.store(itemLists);
  }

  public void compoundSelectorCompoundsChanged(List<String> compounds) {
    chosenCompounds = getStorage().compoundsStorage.store(compounds);
  }
}
