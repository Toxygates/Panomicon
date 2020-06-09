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
import t.common.shared.Dataset;
import t.model.SampleClass;
import t.viewer.client.components.FilterTools;
import t.viewer.client.future.Future;
import t.viewer.client.future.FutureUtils;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Contains some functionality for managing an instance of FilterTools. 
 */
public abstract class FilterScreen extends MinimalScreen {
  protected FilterTools filterTools;
  
  protected FilterScreen(String title, String key, ScreenManager man,
      @Nullable TextResource helpHTML, @Nullable ImageResource helpImage) {
    super(title, key, man, helpHTML, helpImage);
  }
  
  /**
   * Uses a provided future to make an RPC call to set chosen datasets and retrieve
   * the sample classes valid for them, then sets the sample class selection for
   * filterTools. 
   * @param future the future to be used for the RPC call to fetch sampleclasses 
   * @param datasets the set of chosen datasets to be sent to the server 
   * @return the same future that was passed in
   */
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
