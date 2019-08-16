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

import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

import t.common.shared.DataSchema;
import t.model.sample.AttributeSet;
import t.viewer.client.storage.StorageProvider;
import t.viewer.shared.AppInfo;

/**
 * High-level building block for applications. A screen is a GUI with a specific
 * theme or purpose. An application consists of a series of screens.
 */
public interface Screen {
  AppInfo appInfo();
  
  DataSchema schema();
  
  AttributeSet attributes();

  Logger getLogger();

  StorageProvider getStorage();
  
  void addPendingRequest();

  void removePendingRequest();

  //Accessors
  String getTitle();
  String key();
  boolean enabled();
  Widget widget();
  List<MenuItem> menuItems();
  String additionalNavlinkStyle();

  void initGUI();

  void loadState(AttributeSet attributes);

  /**
   * The screen can potentially rebuild itself prior to being shown, by overriding this method.
   * This allows menus and the main content to change in response to saved state.
   */
  default void preShow() {}
  
  void show();

  void hide();

  void resizeInterface();

  void showGuide();

  void showHelp();
  
  void showToolbar(Widget toolbar);
  
  void hideToolbar(Widget toolbar);
}
