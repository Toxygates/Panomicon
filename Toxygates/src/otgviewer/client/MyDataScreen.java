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

import java.util.*;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;

import otgviewer.client.components.DLWScreen;
import otgviewer.client.components.ScreenManager;
import t.common.client.Utils;
import t.common.client.maintenance.*;
import t.common.shared.Dataset;
import t.common.shared.maintenance.Batch;
import t.common.shared.maintenance.Instance;
import t.viewer.client.Analytics;
import t.viewer.client.rpc.UserDataServiceAsync;

public class MyDataScreen extends DLWScreen {

  public static final String key = "my";
  
  private final UserDataServiceAsync userData;
  private final Resources resources; 
  private final ListDataProvider<Batch> batchData = new ListDataProvider<Batch>();
  
  private HorizontalPanel cmds = t.viewer.client.Utils.mkHorizontalPanel();
  
  private String userKey;   
  private String userDataset;
  private String userSharedDataset;
  
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
  
  @Override
  public Widget content() {
    final Set<String> instancesForBatch = new HashSet<String>();       
    instancesForBatch.add(appInfo().instanceName());
    
    BatchPanel bp = new BatchPanel(userData, resources,
        true, true) {
      
      final static String HAS_SEEN_WARNING = "hasSeenMyDataWarning";
      
      @Override
      protected boolean confirmAddNew() {
        String hasSeen = MyDataScreen.this.getParser().getItem(HAS_SEEN_WARNING);
        if ("true".equals(hasSeen)) { // hasSeen might be null
          return true;
        }
        final String message = "NIBIOHN will take reasonable precautions to protect " +
            "your data, but we are not responsible for any data loss, theft or corruption " +
            "that occurs as a result of using this service. " +
            "By proceeding, you confirm that you upload data at your own risk. " +
            "For more details, see the README file provided in the example data above.";
        boolean confirm = Window.confirm(message);
        if (confirm) {
          MyDataScreen.this.getParser().setItem(HAS_SEEN_WARNING, "true");
          return true;
        }
        return false;
      }
      
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
          protected void onBatchUploadBegan() {
            Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT,
                Analytics.ACTION_BEGIN_DATA_UPLOAD);
          }

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
            visList.addItem("Public");
            vp.add(visList);
            if (b != null && Dataset.isSharedDataset(b.getDataset())) {
              visList.setSelectedIndex(1);
            } else {
              visList.setSelectedIndex(0);
            }
          }
          
          @Override
          protected String datasetForBatch() {
            String vis = visList.getSelectedValue();
            if (vis.equals("Private")) {
              return userDataset;
            } else {              
              return userSharedDataset;
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
    keyLabel = new Label("Access key: " + userKey);
    cmds.add(keyLabel);
    Button b = new Button("Change ...");
    b.addClickHandler(new ClickHandler() {      
      @Override
      public void onClick(ClickEvent event) {   
        if (Window.confirm("If you have uploaded any data, please save your existing key first.\n" +
              "Without it, you will lose access to your data. Proceed?")) {
          String newKey = Window.prompt("Please input your user data key.", "");
          if (newKey != null && validateKey(newKey)) {
            setUserKey(newKey);
            refreshBatches();
          } else {
            Window.alert("The string you entered is not a valid user key. "
                + " Please contact us if you need assistance.");
          }
        }
      }
    });
    cmds.add(b);    
    refreshBatches();    
    return bp.table();
  }
  
  private boolean validateKey(String key) {    
    // example: 153f36236251fcea601
    // min size 19 hex chars, max theoretical size 24 chars
    // last 8 chars are random, first 11-16 chars are a timestamp
    return key.matches("[0-9a-f]+") && key.length() >= 19;
  }
  
  private void setUserKey(String key) {
    getParser().setItem("userDataKey", key);    
    userKey = key;
    userDataset = Dataset.userDatasetTitle(key);
    userSharedDataset = Dataset.userSharedDatasetTitle(key);        
    logger.info("The unique user key is: " + key);
    if (keyLabel != null) {
      keyLabel.setText("Your access key is: " + userKey);
    }
  }

  private void refreshBatches() {
    String[] dss = { userDataset, userSharedDataset };
    userData.getBatches(dss, new ListDataCallback<Batch>(batchData, "batch list"));
  }
  
  private void deleteBatch(Batch b) {
    userData.deleteBatchAsync(b, new TaskCallback(logger, "Delete batch", userData) {
      @Override
      public void onCompletion() {
        refreshBatches();
      }          
    });
  }
}
