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

package t.viewer.client.future;

import java.util.logging.Level;

import com.google.gwt.user.client.Window;

import t.viewer.client.components.Screen;

/**
 * Utility methods for Futures
 */
public class FutureUtils {
  private FutureUtils() {} // Prevent instantiation of this class 
 
  /**
   * Helper method to do the following for a future:
   * 1) increment the screen's pending request counter, and decrement it when the future
   * completes.
   * 2) if the future completes with an error, display it in a popup
   * @param future the future to add a callback to
   * @param screen the screen whose pending requests counter should be modified, and in 
   * which an error message should be shown if necessary 
   * @param errorMessage the message to show before the future's throwable's message, in
   * case the future completes with an error
   * @return
   */
  public static <T> Future<T> beginPendingRequestHandling(Future<T> future, Screen screen, String errorMessage) {
    screen.addPendingRequest();
    future.addCallback(f -> {
      screen.removePendingRequest();
      if (f.doneWithError()) {
        screen.getLogger().log(Level.SEVERE, errorMessage, f.caught());
        Window.alert(errorMessage + ": " + f.caught());
      }
    });
    return future;
  }
}
