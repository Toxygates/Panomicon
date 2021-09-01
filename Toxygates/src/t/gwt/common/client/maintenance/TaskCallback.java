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

package t.gwt.common.client.maintenance;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;

import t.gwt.common.client.rpc.MaintenanceOperationsAsync;

public class TaskCallback implements AsyncCallback<Void> {
  final String title;
  final MaintenanceOperationsAsync maintenanceOps;
  final Logger log;
  
  public TaskCallback(Logger l, String title, MaintenanceOperationsAsync maintenanceOps) {
    this.log = l;
    this.title = title;
    this.maintenanceOps = maintenanceOps;
  }

  @Override
  public void onSuccess(Void result) {
    final DialogBox db = new DialogBox(false, true);
    ProgressDisplay pd = new ProgressDisplay(title, maintenanceOps) {
      @Override
      protected void onDone(boolean success) {
        db.hide();
        if (success) {
          onCompletion();
        } else {
          TaskCallback.this.onCancelled();
        }
      }
    };
    db.setWidget(pd);
    db.setText("Progress");
    db.show();
  }

  @Override
  public void onFailure(Throwable caught) {
    Window.alert("Failure: " + caught);

    log.log(Level.SEVERE, "TaskCallback error", caught);
    if (caught.getCause() != null) {
      log.log(Level.SEVERE, "TaskCallback cause", caught.getCause());
    }

    handleFailure(caught);
  }

  protected void onCompletion() {

  }

  protected void onCancelled() {

  }

  protected void handleFailure(Throwable caught) {

  }

}
