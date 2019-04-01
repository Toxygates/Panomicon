package otg.viewer.client.components;

import java.util.List;

import javax.annotation.Nullable;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

import t.common.shared.Dataset;
import t.model.SampleClass;
import t.viewer.client.future.Future;
import t.viewer.client.future.FutureUtils;

public abstract class FilterScreen extends MinimalScreen {
  protected FilterTools filterTools;
  
  protected FilterScreen(String title, String key, ScreenManager man,
      @Nullable TextResource helpHTML, @Nullable ImageResource helpImage) {
    super(title, key, man, helpHTML, helpImage);
  }
  
  public Future<SampleClass[]> fetchSampleClasses(Future<SampleClass[]> future,
      List<Dataset> datasets) {
    logger.info("Request sample classes for " + datasets.size() + " datasets");
    manager().sampleService().chooseDatasets(datasets.toArray(new Dataset[0]), future);
    FutureUtils.beginPendingRequestHandling(future, this, "Unable to choose datasets and fetch sample classes");
    future.addSuccessCallback(sampleClasses -> {
      logger.info("sample classes fetched");
      filterTools.dataFilterEditor.setAvailable(sampleClasses);
    });
    return future;
  }
}
