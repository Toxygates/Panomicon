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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.FilterTools;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.compoundsel.CompoundSelector;
import otgviewer.client.components.groupdef.GroupInspector;
import t.common.shared.DataSchema;
import t.common.shared.sample.Group;
import t.common.shared.sample.SampleColumn;
import t.model.sample.AttributeSet;
import t.viewer.client.StorageParser;
import t.viewer.client.Utils;

/**
 * This screen allows for column (group) definition as well as compound ranking.
 */
public class ColumnScreen extends DataFilterScreen {
  public static String key = "columns";

  private GroupInspector gi;
  private CompoundSelector cs;
  private FilterTools filterTools;

  public ColumnScreen(ScreenManager man) {
    super("Sample groups", key, false, man, man.resources().groupDefinitionHTML(), 
        man.resources().groupDefinitionHelp());

    cs = new CompoundSelector(this, man.schema().majorParameter().title(), true, true) {
      @Override
      public void changeCompounds(List<String> compounds) {
        super.changeCompounds(compounds);
        storeCompounds(getParser(ColumnScreen.this));
      }
    };
    this.addListener(cs);
    cs.addStyleName("compoundSelector");

    chosenDatasets = appInfo().datasets();
    filterTools = new FilterTools(this);
    this.addListener(filterTools);
  }

  @Override
  protected void addToolbars() {   
    super.addToolbars();   
    HorizontalPanel hp = Utils.mkHorizontalPanel(false, filterTools);
    addToolbar(hp, 0);
    addLeftbar(cs, 350);
  }

  @Override
  protected boolean shouldShowStatusBar() {
    return false;
  }

  @Override
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

    Button b = new Button("Delete all groups",
        (ClickHandler) e -> gi.confirmDeleteAllGroups());     

    Button b2 = new Button("Next: View data", 
        (ClickHandler) e -> {
        if (gi.chosenColumns().size() == 0) {
          Window.alert("Please define and activate at least one group.");
        } else {
          configuredProceed(DataScreen.key);
        }
      });

    hp.add(Utils.mkHorizontalPanel(true, b, b2));
    return hp;
  }

  @Override
  public void loadState(StorageParser p, DataSchema schema, AttributeSet attributes) {
    super.loadState(p, schema, attributes);
    if (visible) {
      try {
        List<Group> ics =
            loadColumns(p, schema(), "inactiveColumns", new ArrayList<SampleColumn>(gi
                .existingGroupsTable().inverseSelection()), attributes());
        if (ics != null && ics.size() > 0) {
          logger.info("Unpacked i. columns: " + ics.get(0) + ": " + ics.get(0).getSamples()[0]
              + " ... ");
          gi.inactiveColumnsChanged(ics);
        } else {
          logger.info("No inactive columns available");
        }

      } catch (Exception e) {
        logger.log(Level.WARNING, "Unable to load inactive columns", e);
        Window.alert("Unable to load inactive columns.");
      }
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
