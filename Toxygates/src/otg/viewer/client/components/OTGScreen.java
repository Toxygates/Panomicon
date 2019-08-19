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

package otg.viewer.client.components;

import otg.viewer.client.Resources;
import otg.viewer.client.UIFactory;
import t.common.shared.DataSchema;
import t.model.sample.AttributeSet;
import t.viewer.client.components.Screen;
import t.viewer.client.storage.StorageProvider;
import t.viewer.shared.AppInfo;

public interface OTGScreen extends Screen {
  default UIFactory factory() {
    return manager().factory();
  }

  default Resources resources() {
    return manager().resources();
  }

  default StorageProvider getStorage() {
    return manager().getStorage();
  }
  
  default AppInfo appInfo() {
    return manager().appInfo();
  }

  default DataSchema schema() {
    return manager().schema();
  }

  default AttributeSet attributes() {
    return manager().appInfo().attributes();
  }

  // Accessors
  ScreenManager manager();
}
