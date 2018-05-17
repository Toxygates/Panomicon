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

package otgviewer.client;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

import otgviewer.client.components.DLWScreen;
import otgviewer.client.components.ScreenManager;
import t.model.SampleClass;

/**
 * A screen that supports FilterTools. More code may be moved here from the various sub-screens
 * in the future.
 */
abstract public class DataFilterScreen extends DLWScreen {
    
  public DataFilterScreen(String title, String key, boolean showGroups, ScreenManager man,
      TextResource helpHTML, ImageResource helpImage) {
    super(title, key, showGroups, man, helpHTML, helpImage);
  }
  @Override
  public void changeSampleClass(SampleClass sc) {
    // On this screen, ignore the blank sample class set by
    // DataListenerWidget
    if (!sc.getMap().isEmpty()) {
      super.changeSampleClass(sc);
      storeSampleClass(getParser());
    }
  }

  private boolean initialised = false;
  @Override 
  public void show() {
    super.show();
    if (!initialised) {
      //Force reloading of sample classes
      changeDatasets(chosenDatasets);
      initialised = true;
    }
  }

  // FilterTools.Delegate method
  public void filterToolsSampleClassChanged(SampleClass sc) {
    sampleClassChanged(sc);
    runActions();
  }
}
