/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package t.admin.client;

import t.admin.shared.Batch;
import t.admin.shared.Instance;
import t.admin.shared.OperationResults;
import t.admin.shared.Platform;
import t.admin.shared.Progress;
import t.common.shared.Dataset;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MaintenanceServiceAsync {

	void getBatches(AsyncCallback<Batch[]> callback);
	
	void getInstances(AsyncCallback<Instance[]> callback);
	
	void getPlatforms(AsyncCallback<Platform[]> callback);
	
	void getDatasets(AsyncCallback<Dataset[]> callback);
	
	void addBatchAsync(String id, String comment, AsyncCallback<Void> callback);
	
	void addPlatformAsync(String id, String comment, boolean affymetrixFormat, 
			AsyncCallback<Void> callback);
	
	void addInstance(Instance i, AsyncCallback<Void> callback);

	void addDataset(Dataset d, AsyncCallback<Void> callback);
	
	
	void deleteBatchAsync(String id, AsyncCallback<Void> callback);
	
	void deletePlatformAsync(String id, AsyncCallback<Void> calback);
	
	void deleteInstance(String id, AsyncCallback<Void> callback);
	
	void deleteDataset(String id, AsyncCallback<Void> callback);
	
	
	void updateBatch(Batch b, AsyncCallback<Void> callback);
	
	void getOperationResults(AsyncCallback<OperationResults> callback);
	
	void cancelTask(AsyncCallback<Void> callback);
	
	void getProgress(AsyncCallback<Progress> callback);
}
