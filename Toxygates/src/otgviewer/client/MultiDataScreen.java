/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package otgviewer.client;

import java.util.*;

import otgviewer.client.components.*;
import t.common.shared.GroupUtils;
import t.viewer.client.table.DualTableView;
import t.viewer.client.table.TableView;

/**
 * The MultiDataScreen picks one of several available DataScreens
 * depending on the configured state.
 * This screen itself has no content and will not be shown.
 */
public class MultiDataScreen extends DLWScreen implements ImportingScreen {

  private DataScreen singleTableScreen, dualTableScreen;
  
  public MultiDataScreen(ScreenManager man) {
    super("View data", DataScreen.key, false, man);
    
    //TODO update wiring to yuji's new system
    singleTableScreen = new DataScreen(man);
    addListener(singleTableScreen);
    
    dualTableScreen = new DataScreen(man) {
      @Override
      protected TableView makeDataView() {
        return new DualTableView(this, mainTableTitle());
      }     
      //Needed?
//    @Override
//    protected void beforeGetAssociations() {
//      super.beforeGetAssociations();
//      DataScreen.this.beforeGetAssociations();
//    }
    };
    addListener(dualTableScreen);
  }

  public Screen preferredReplacement() {
    String[] types =
        chosenColumns.stream().map(g -> GroupUtils.groupType(g)).distinct().toArray(String[]::new);
    return types.length >= 2 ? dualTableScreen: singleTableScreen;
  }

  @Override
  public boolean enabled() {
    return singleTableScreen.enabled() || dualTableScreen.enabled();
  }

  @Override
  public void initGUI() {
    singleTableScreen.initGUI();
    dualTableScreen.initGUI();
  }
  
  @Override
  public Collection<Screen> potentialReplacements() {
    return Arrays.asList(singleTableScreen, dualTableScreen);
  }
  
}
