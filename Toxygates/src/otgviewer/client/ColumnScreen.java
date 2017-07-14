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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import otgviewer.client.components.FilterTools;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.StorageParser;
import otgviewer.client.components.compoundsel.CompoundSelector;
import otgviewer.client.components.groupdef.GroupInspector;
import t.common.shared.DataSchema;
import t.model.SampleClass;
import t.common.shared.sample.Group;
import t.common.shared.sample.SampleColumn;
import t.viewer.client.Utils;
import t.viewer.client.components.search.SearchDialog;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.SampleServiceAsync;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This screen allows for column (group) definition as well as compound ranking.
 */
public class ColumnScreen extends Screen {
  public static String key = "columns";

  private GroupInspector gi;
  private CompoundSelector cs;
  private FilterTools filterTools;
  private SampleServiceAsync sampleService;

  public ColumnScreen(ScreenManager man) {
    super("Sample groups", key, false, man, resources.groupDefinitionHTML(), resources
        .groupDefinitionHelp());
    
    this.sampleService = man.sampleService();

    String majorParam = man.schema().majorParameter();
    cs = new CompoundSelector(this, man.schema().title(majorParam), true, true) {
      @Override
      public void changeCompounds(List<String> compounds) {
        super.changeCompounds(compounds);
        storeCompounds(getParser(ColumnScreen.this));
      }
    };
    this.addListener(cs);
    cs.setStylePrimaryName("compoundSelector");

    chosenDatasets = appInfo().datasets();
    filterTools = new FilterTools(this);
    this.addListener(filterTools);
  }

  @Override
  protected void addToolbars() {   
    super.addToolbars();   
    HorizontalPanel hp = Utils.mkHorizontalPanel(true, filterTools);
   
    boolean isDev = manager().appInfo().instanceName().equals("dev");
    if (isDev) {
      Button searchButton = new Button("Search...");
      searchButton.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent arg0) {
          search();
        }
      });
      hp.add(searchButton);
    }
   
    addToolbar(hp, 45);
    addLeftbar(cs, 350);
  }

  @Override
  protected boolean shouldShowStatusBar() {
    return false;
  }

  public Widget content() {
    gi = factory().groupInspector(cs, this);
    this.addListener(gi);
    cs.addListener(gi);
    gi.datasetsChanged(chosenDatasets);
    gi.addStaticGroups(appInfo().predefinedSampleGroups());
    return gi;
  }

  @Override
  public Widget bottomContent() {
    HorizontalPanel hp = Utils.mkWidePanel();

    Button b = new Button("Delete all groups", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        gi.confirmDeleteAllGroups();
      }
    });

    Button b2 = new Button("Next: View data", new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (gi.chosenColumns().size() == 0) {
          Window.alert("Please define and activate at least one group.");
        } else {
          configuredProceed(DataScreen.key);
        }
      }
    });

    hp.add(Utils.mkHorizontalPanel(true, b, b2));
    return hp;
  }

  @Override
  public void loadState(StorageParser p, DataSchema schema) {
    super.loadState(p, schema);
    if (visible) {
      try {
        List<Group> ics =
            loadColumns(p, schema(), "inactiveColumns", new ArrayList<SampleColumn>(gi
                .existingGroupsTable().inverseSelection()));
        if (ics != null && ics.size() > 0) {
          logger.info("Unpacked i. columns: " + ics.get(0) + ": " + ics.get(0).getSamples()[0]
              + " ... ");
          gi.inactiveColumnsChanged(ics);
        } else {
          logger.info("No i. columns available");
        }

      } catch (Exception e) {
        logger.log(Level.WARNING, "Unable to load i. columns", e);
        Window.alert("Unable to load inactive columns.");
      }
    }
  }
  
  protected void search() {
    SearchDialog sd = new SearchDialog(appInfo(),
        sampleService, chosenSampleClass);
    Utils.displayInPopup("Sample search conditions", 
        sd, DialogPosition.Center);
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

  @Override
  public void tryConfigure() {
    if (chosenColumns.size() > 0) {
      setConfigured(true);
    }
  }

  @Override
  public void resizeInterface() {
    // Test carefully in IE8, IE9 and all other browsers if changing this method
    cs.resizeInterface();
    super.resizeInterface();
  }

  @Override
  public String getGuideText() {
    return "Please define at least one sample group to proceed. Start by selecting compounds to the left. Then select doses and times.";
  }
}
