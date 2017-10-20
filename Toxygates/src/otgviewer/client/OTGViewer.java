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

import otgviewer.client.dialog.MirnaSourceDialog;
import otgviewer.shared.OTGSchema;
import t.common.shared.DataSchema;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.intermine.InterMineData;
import t.viewer.shared.intermine.IntermineInstance;
import t.viewer.shared.mirna.MirnaSource;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

public class OTGViewer extends TApplication {

  @Override
  protected void initScreens() {
    addScreenSeq(new StartScreen(this));
    addScreenSeq(new ColumnScreen(this));
    addScreenSeq(new SampleSearchScreen(this));
    addScreenSeq(new DualDataScreen(this));
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
    
    MenuItem mi = new MenuItem("Select MiRNA sources...", new Command() {
      @Override
      public void execute() {
        MirnaSource[] sources = appInfo().mirnaSources();
        new MirnaSourceDialog(currentScreen, probeService(), sources).
          display("Choose miRNA sources", DialogPosition.Center);
      }      
    });
    
    toolsMenuBar.addItem(mi);
  }
  
  protected MenuItem intermineMenu(final IntermineInstance inst) {
    MenuBar mb = new MenuBar(true);
    final String title = inst.title();
    MenuItem mi = new MenuItem(title + " data", mb);

    mb.addItem(new MenuItem("Import gene sets from " + title + "...", new Command() {
      @Override
      public void execute() {
        new InterMineData(currentScreen, inst).importLists(true);
        Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT, Analytics.ACTION_IMPORT_GENE_SETS,
            title);
      }
    }));

    mb.addItem(new MenuItem("Export gene sets to " + title + "...", new Command() {
      @Override
      public void execute() {
        new InterMineData(currentScreen, inst).exportLists();
        Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT, Analytics.ACTION_EXPORT_GENE_SETS,
            title);
      }
    }));

    mb.addItem(new MenuItem("Enrichment...", new Command() {
      @Override
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
      @Override
      public void execute() {
        Utils.displayURL("Go to " + title + " in a new window?", "Go", inst.webURL());
      }
    }));
    return mi;
  }
}
