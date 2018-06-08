/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package otgviewer.client.components;

import java.util.*;
import java.util.logging.Logger;

import otgviewer.client.Resources;
import otgviewer.client.UIFactory;
import t.common.shared.DataSchema;
import t.model.sample.AttributeSet;
import t.viewer.client.StorageParser;
import t.viewer.shared.AppInfo;

import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

public interface Screen {
  default AppInfo appInfo() {
    return manager().appInfo();
  }

  default DataSchema schema() {
    return manager().schema();
  }

  default UIFactory factory() {
    return manager().factory();
  }

  default Resources resources() {
    return manager().resources();
  }

  default AttributeSet attributes() {
    return manager().appInfo().attributes();
  }

  default StorageParser getParser() {
    return manager().getParser();
  }

  // Accessors
  ScreenManager manager();
  Logger getLogger();
  String getTitle();
  String key();
  boolean enabled();
  Widget widget();
  List<MenuItem> menuItems();
  String additionalNavlinkStyle();

  void initGUI();

  void tryConfigure();

  void setConfigured(boolean cfg);

  void loadState(AttributeSet attributes);

  void loadPersistedState();

  void show();

  void hide();

  void resizeInterface();

  void showGuide();

  void showHelp();

  void addPendingRequest();

  void removePendingRequest();
  
  /**
   * This screen may optionally replace itself with another one when being shown,
   * based on the current stored state. Such a replacement screen should be returned here.
   * If no replacement is desired, the screen should return itself.
   */
  default Screen preferredReplacement() {
    return this;
  }
  
  /**
   * A collection of all potential replacements for this screen. (See above)
   */
  default Collection<Screen> potentialReplacements() {
    return new ArrayList<Screen>();
  }
}
