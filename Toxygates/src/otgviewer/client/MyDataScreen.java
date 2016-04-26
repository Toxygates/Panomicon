/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import t.common.client.Utils;
import t.common.client.maintenance.BatchEditor;
import t.common.client.maintenance.BatchPanel;
import t.common.client.maintenance.ListDataCallback;
import t.common.client.maintenance.TaskCallback;
import t.common.shared.Dataset;
import t.common.shared.maintenance.Batch;
import t.common.shared.maintenance.Instance;
import t.viewer.client.rpc.UserDataServiceAsync;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class MyDataScreen extends Screen {

  public static final String key = "my";
  
  private final UserDataServiceAsync userData;
  private final Resources resources; 
  private final ListDataProvider<Batch> batchData = new ListDataProvider<Batch>();
  
  private HorizontalPanel cmds = t.viewer.client.Utils.mkHorizontalPanel();
  
  private String userKey;   
  private String userDataset;
  
  private Label keyLabel;
  
  public MyDataScreen(ScreenManager man) {
    super("My data", key, false, man);
    userData = man.userDataService();
    resources = man.resources();
    addToolbar(cmds, 35);
    
    String key = getParser().getItem("userDataKey");
    if (key == null) {
      key = manager().appInfo().getUserKey();
    }
    setUserKey(key);    
  }
  
  public Widget content() {
    //Default instances for new user datasets. If the instance is not listed here,
    //the user will not be able to see their data.
    final Set<String> instancesForBatch = new HashSet<String>();    
    instancesForBatch.add("dev");
    instancesForBatch.add("adjuvant");
    instancesForBatch.add("toxygates");
    instancesForBatch.add("toxygates-test");
    
    BatchPanel bp = new BatchPanel("Edit batch", userData, resources,
        true, true) {
      
      @Override
      protected void onDelete(Batch object) {
        if (Window.confirm("Are you sure?")) {
          deleteBatch(object);
        }
      }
      
      @Override
      protected Widget makeEditor(Batch b, final DialogBox db, boolean addNew) {
        return new BatchEditor(b, addNew, new ArrayList<Dataset>(), 
            new ArrayList<Instance>(), userData) {
          ListBox visList;
          
          @Override
          protected void onFinishOrAbort() {
            db.hide();
            doRefresh();
          }
          
          @Override
          protected void guiBeforeUploader(VerticalPanel vp, Batch b, boolean addNew) {
            visList = new ListBox();
            vp.add(new Label("Visibility"));
            visList.addItem("Private");
            visList.addItem("Shared");
            vp.add(visList);
          }
          
          @Override
          protected String datasetForBatch() {
            String vis = visList.getSelectedValue();
            if (vis.equals("Private")) {
              return userDataset;
            } else {              
              return "user-shared"; //TODO
            }
          }
          
          @Override
          protected Set<String> instancesForBatch() {
            return instancesForBatch;
          }
        };
      }
      
      @Override
      protected void doRefresh() {
        refreshBatches();        
      }
      
      @Override
      protected boolean hasVisibility() {
        return false;
      }
    };   
    batchData.addDataDisplay(bp.table());
    cmds.setSpacing(10);
    cmds.add(Utils.makeButtons(bp.commands()));
    
    HTML h = new HTML();
    h.setHTML("<a target=_blank href=\"Toxygates user data example.zip\"> Download example files</a>");
    cmds.add(h);
    keyLabel = new Label("Your access key is: " + userKey); 
    cmds.add(keyLabel);
    Button b = new Button("Set...");
    b.addClickHandler(new ClickHandler() {      
      @Override
      public void onClick(ClickEvent event) {   
        if (Window.confirm("If you have uploaded any data, please save your existing key first.\n" +
              "Without it, you will lose access to your data. Proceed?")) {
          String newKey = Window.prompt("Please input your user data key.", "");
          if (newKey != null) {
            setUserKey(newKey);
            refreshBatches();
          }
        }
      }
    });
    cmds.add(b);    
    refreshBatches();    
    return bp.table();
  }
  
  private void setUserKey(String key) {
    getParser().setItem("userDataKey", key);    
    userKey = key;
    userDataset = "user-" + key;    
    logger.info("The unique user key is: " + userKey);
    if (keyLabel != null) {
      keyLabel.setText("Your access key is: " + userKey);
    }
  }

  private void refreshBatches() {
    userData.getBatches(userDataset, new ListDataCallback<Batch>(batchData, "batch list"));
  }
  
  private void deleteBatch(Batch b) {
    userData.deleteBatchAsync(b.getTitle(), new TaskCallback("Delete batch", userData) {      
      public void onCompletion() {
        refreshBatches();
      }          
    });
  }
}
