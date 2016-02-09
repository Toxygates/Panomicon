/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.admin.client;

import t.common.shared.Dataset;
import t.common.shared.ManagedItem;
import t.common.shared.Platform;
import t.common.shared.maintenance.Batch;
import t.common.shared.maintenance.Instance;
import t.viewer.client.rpc.BatchOperationsAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MaintenanceServiceAsync extends BatchOperationsAsync {

  void getInstances(AsyncCallback<Instance[]> callback);

  void getPlatforms(AsyncCallback<Platform[]> callback);

  void getDatasets(AsyncCallback<Dataset[]> callback);

  void addBatchAsync(Batch b, AsyncCallback<Void> callback);

  void addPlatformAsync(Platform p, boolean affymetrixFormat, AsyncCallback<Void> callback);

  void add(ManagedItem i, AsyncCallback<Void> callback);

  void deletePlatformAsync(String id, AsyncCallback<Void> calback);

  void delete(ManagedItem i, AsyncCallback<Void> callback);

}
