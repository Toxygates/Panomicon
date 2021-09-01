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

package t.gwt.common.client.rpc;

import javax.annotation.Nullable;

import com.google.gwt.user.client.rpc.AsyncCallback;

import t.shared.common.Dataset;
import t.shared.common.maintenance.Batch;
import t.model.sample.Attribute;

public interface BatchOperationsAsync extends MaintenanceOperationsAsync {
  
  void getBatches(@Nullable String[] datasets, AsyncCallback<Batch[]> callback);
  
  void addBatchAsync(Batch b, AsyncCallback<Void> callback);

  void updateBatchMetadataAsync(Batch b, boolean recalculate, AsyncCallback<Void> callback);

  void batchAttributeSummary(Batch b, AsyncCallback<String[][]> callback);

  void datasetSampleSummary(Dataset d, Attribute[] rowAttributes,
                            Attribute[] columnAttributes,
                            @Nullable Attribute cellValue,
                            AsyncCallback<String[][]> callback);
  
  void deleteBatchAsync(Batch b, AsyncCallback<Void> callback);

}