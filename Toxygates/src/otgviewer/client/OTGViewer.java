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

package otgviewer.client;

import otgviewer.client.intermine.TargetMineData;
import otgviewer.shared.OTGSchema;
import otgviewer.shared.intermine.IntermineInstance;
import t.common.shared.DataSchema;
import t.viewer.client.Utils;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

public class OTGViewer extends TApplication {

  @Override
  protected void initScreens() {
    addScreenSeq(new StartScreen(this));
    addScreenSeq(new ColumnScreen(this));
    addScreenSeq(new DataScreen(this));
    addScreenSeq(new RankingScreen(this));
    addScreenSeq(new PathologyScreen(this));
    addScreenSeq(new SampleDetailScreen(this));
    
    if (factory.hasMyData()) {      
      addScreenSeq(new MyDataScreen(this));
    }
  }

  final private OTGSchema schema = new OTGSchema();

  @Override
  public DataSchema schema() {
    return schema;
  }

  final private UIFactory factory = initFactory();

  private UIFactory initFactory() {
    //TODO hardcoding these instance names here may be controversial
    // - think of a better way of handling this
    UIFactory f;
    if (instanceName().equals("toxygates") ||
        instanceName().equals("tg-update")) {
      f = new ClassicOTGFactory();
    } else {
      f = new OTGFactory();
    }    
    logger.info("Using factory: " + f.getClass().toString());
    return f;
  }
  
  @Override
  public UIFactory factory() {    
    return factory;
  }

  @Override
  protected void setupToolsMenu(MenuBar toolsMenuBar) {
    for (IntermineInstance ii: appInfo.intermineInstances()) {
      toolsMenuBar.addItem(intermineMenu(ii));
    }  
  }
  
  protected MenuItem intermineMenu(final IntermineInstance inst) {
    MenuBar mb = new MenuBar(true);
    final String title = inst.title();
    MenuItem mi = new MenuItem(title + " data", mb);

    mb.addItem(new MenuItem("Import gene sets from " + title + "...", new Command() {
      public void execute() {
        new TargetMineData(currentScreen, inst).importLists(true);
      }
    }));

    mb.addItem(new MenuItem("Export gene sets to " + title + "...", new Command() {
      public void execute() {
        new TargetMineData(currentScreen, inst).exportLists();
      }
    }));

    mb.addItem(new MenuItem("Enrichment...", new Command() {
      public void execute() {
        //TODO this should be disabled if we are not on the data screen.
        //The menu item is only here in order to be logically grouped with other 
        //TargetMine items, but it is a duplicate and may be removed.
        if (currentScreen instanceof DataScreen) {
          ((DataScreen) currentScreen).runEnrichment(inst);
        } else {
          Window.alert("Please go to the data screen to use this function.");
        }
      }
    }));

    mb.addItem(new MenuItem("Go to " + title, new Command() {
      public void execute() {
        Utils.displayURL("Go to " + title + " in a new window?", "Go", inst.webURL());
      }
    }));
    return mi;
  }
}
