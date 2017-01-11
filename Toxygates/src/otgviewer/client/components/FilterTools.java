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

package otgviewer.client.components;

import java.util.Arrays;

import t.common.shared.Dataset;
import t.common.shared.SampleClass;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.shared.AppInfo;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;

public class FilterTools extends DataListenerWidget {
  private HorizontalPanel filterTools;
  private DataFilterEditor dfe;
  final Screen screen;
  final SampleServiceAsync sampleService;

  public FilterTools(final Screen screen) {
    this.screen = screen;
    sampleService = screen.manager().sampleService();

    filterTools = new HorizontalPanel();
    initWidget(filterTools);

    filterTools.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    filterTools.addStyleName("slightlySpacedLeftRight");

    Button b = new Button("Data...");
    filterTools.add(b);
    b.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        showDatasetSelector();
      }
    });

    dfe = new DataFilterEditor(screen) {
      @Override
      protected void changeSampleClass(SampleClass sc) {
        super.changeSampleClass(sc);
        screen.sampleClassChanged(sc);

        // TODO Actions are enqueued in TimeDoseGrid and CompoundSelector.
        // I'm not sure that exposing the action queue mechanism
        // like this is a good thing to do. Think of a better way.
        screen.runActions();
      }
    };
    this.addListener(dfe);
    filterTools.add(dfe);
  }

  protected void showDatasetSelector() {    
    //Re-retrieve user data here (in case user key has changed) and re-populate
    //datasets
    
    screen.manager().reloadAppInfo(new AsyncCallback<AppInfo>() {      
      @Override
      public void onSuccess(AppInfo result) {
        proceedShowSelector(result);
      }
      
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Failed to load datasets from server");        
      }
    });    
  }
  
  protected void proceedShowSelector(AppInfo info) {    
    final DialogBox db = new DialogBox(false, true);
    
    // TODO set init. selection
    DatasetSelector dsel =
        new DatasetSelector(Arrays.asList(info.datasets()),
            Arrays.asList(chosenDatasets)) {
          @Override
          public void onOK() {
            datasetsChanged(getSelected().toArray(new Dataset[0]));
            sampleService.chooseDatasets(chosenDatasets, new PendingAsyncCallback<Void>(screen,
                "Unable to choose datasets") {
              public void handleSuccess(Void v) {
                dfe.update();
              }
            });
            db.hide();
          }

          @Override
          public void onCancel() {
            super.onCancel();
            db.hide();
          }
        };
    db.setText("Select datasets");
    db.setWidget(dsel);
    db.setWidth("500px");
    db.show();
  }

  @Override
  public void datasetsChanged(Dataset[] ds) {
    boolean realChange = !Arrays.equals(ds, chosenDatasets);
    super.datasetsChanged(ds);
    storeDatasets(getParser(screen));
    if (realChange) {
      dfe.update();
    }
  }

}
