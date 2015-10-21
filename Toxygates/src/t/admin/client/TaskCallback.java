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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;

public class TaskCallback implements AsyncCallback<Void> {
  final String title;

  public TaskCallback(String title) {
    this.title = title;
  }

  @Override
  public void onSuccess(Void result) {
    final DialogBox db = new DialogBox(false, true);
    ProgressDisplay pd = new ProgressDisplay(title) {
      @Override
      protected void onDone() {
        db.hide();
        onCompletion();
      }

      @Override
      protected void onCancelled() {
        onFailure();
      }
    };
    db.setWidget(pd);
    db.setText("Progress");
    db.show();
  }

  @Override
  public void onFailure(Throwable caught) {
    Window.alert("Failure: " + caught.getMessage());
    onFailure();
  }

  void onCompletion() {

  }

  void onFailure() {

  }

}
