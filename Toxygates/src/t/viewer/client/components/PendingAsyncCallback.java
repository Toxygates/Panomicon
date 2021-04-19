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

package t.viewer.client.components;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import t.viewer.client.screen.ScreenManager;

import javax.annotation.Nullable;
import java.util.logging.Level;

/**
 * Callback that interacts with a screen to display a "please wait" popup.
 */
public class PendingAsyncCallback<T> implements AsyncCallback<T> {

  public interface SuccessAction<T> {
    void run(T t);
  }

  private ScreenManager manager;
  private String onErrorMessage;
  private @Nullable SuccessAction<T> success = null;

  private T result;
  private boolean completedSuccessfully = false;
  
  /**
   * Construct a PendingAsyncCallback. If no SuccessAction is passed in, handleSuccess should be
   * overridden.
   * 
   * @param screenManager Used to display wait popup
   * @param _onErrorMessage
   */
  public PendingAsyncCallback(ScreenManager screenManager, String _onErrorMessage) {
    manager = screenManager;
    onErrorMessage = _onErrorMessage;
    manager.addPendingRequest();
  }

  /**
   * Construct a PendingAsyncCallback. As a syntactic convenience, this constructor allows a lambda
   * to be used.
   * 
   * @param screenManager Used to display wait popup
   * @param _onErrorMessage
   * @param onSuccess callback to run on successful completion.
   */
  public PendingAsyncCallback(ScreenManager screenManager, String _onErrorMessage, SuccessAction<T> onSuccess) {
    manager = screenManager;
    onErrorMessage = _onErrorMessage;
    success = onSuccess;
    manager.addPendingRequest();
  }


  public PendingAsyncCallback(ScreenManager _widget) {
    this(_widget, "There was a server-side error.");
  }

  @Override
  public void onSuccess(T t) {
    completedSuccessfully = true;
    result = t;
    handleSuccess(t);
    manager.removePendingRequest();
  }
  
  public boolean wasSuccessful() {
    return completedSuccessfully;
  }
  
  public T result() {
    return result;
  }

  /**
   * Returns true if this is PendingAsyncCallback is the callback for the last
   * pending request sent by the screen. In other words, , if true, the
   * "Please wait..." dialog will be hidden after this callback finishes.
   */
  public boolean isTheLastCallback() {
    return manager.numPendingRequests() == 1;
  }

  public void handleSuccess(T t) {
    if (success != null) {
      success.run(t);
    }
  }

  @Override
  public void onFailure(Throwable caught) {
    handleFailure(caught);
    manager.removePendingRequest();
  }

  public void handleFailure(Throwable caught) {
    Window.alert(onErrorMessage + ": " + caught);
    manager.getLogger().log(Level.SEVERE, onErrorMessage, caught);
  }

}
