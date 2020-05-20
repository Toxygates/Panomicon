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

package t.viewer.client.charts;

import t.viewer.client.screen.Screen;
import t.common.shared.DataSchema;
import t.common.shared.SharedUtils;
import t.model.SampleClass;
import t.viewer.client.charts.google.GVizFactory;
import t.viewer.client.storage.StorageProvider;

import java.util.logging.Logger;

/**
 * Routines to help construct and display charts. Some charts are interactive, and may fetch data
 * repeatedly based on choices made by the user. Concrete subclasses of this class are the public
 * entry points to the charts package.
 */
abstract class Charts {

  final static int DEFAULT_CHART_GRID_WIDTH = 600;

  protected final Logger logger = SharedUtils.getLogger("charts");

  final protected DataSchema schema;
  protected StorageProvider storageProvider;
  /*
   * Note: ideally this should be instantiated/chosen by some dependency injection system
   */
  protected GVizFactory factory = new GVizFactory();
  protected Screen screen;
  protected SampleClass[] sampleClasses;

  protected Charts(Screen screen) {
    schema = screen.manager().schema();
    storageProvider = screen.getStorage();
    this.screen = screen;
  }
}
