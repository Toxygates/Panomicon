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

package otgviewer.client.components;

import java.util.logging.Level;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class PendingAsyncCallback<T> implements AsyncCallback<T> {

  private Screen screen;
  private String onErrorMessage;

  public PendingAsyncCallback(Screen _widget, String _onErrorMessage) {
    screen = _widget;
    onErrorMessage = _onErrorMessage;
    screen.addPendingRequest();
  }

  public PendingAsyncCallback(Screen _widget) {
    this(_widget, "There was a server-side error.");
  }

  @Override
  public void onSuccess(T t) {
    handleSuccess(t);
    screen.removePendingRequest();
  }

  public void handleSuccess(T t) {
    // Quiet success
  }

  @Override
  public void onFailure(Throwable caught) {
    handleFailure(caught);
    screen.removePendingRequest();
  }

  public void handleFailure(Throwable caught) {
    Window.alert(onErrorMessage + ":" + caught.getMessage());
    screen.getLogger().log(Level.SEVERE, onErrorMessage, caught);
  }

}
