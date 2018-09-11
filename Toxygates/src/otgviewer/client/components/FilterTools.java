/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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
import java.util.logging.Logger;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import t.common.client.Utils;
import t.common.shared.Dataset;
import t.model.SampleClass;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.shared.AppInfo;

/**
 * Tools to select from the available datasets, and then from
 * the available sample macro classes within those datasets.
 */
public class FilterTools extends Composite {
  private HorizontalPanel filterTools;
  private DataFilterEditor dfe;
  final Screen screen;
  final Delegate delegate;
  final SampleServiceAsync sampleService;

  private Logger logger;

  protected Dataset[] chosenDatasets = new Dataset[0];

  public interface Delegate {
    void filterToolsSampleClassChanged(SampleClass sc);
    void filterToolsDatasetsChanged(Dataset[] ds);
  }

  public <T extends Screen & Delegate> FilterTools(T screen) {
    this(screen, screen);
  }

  public FilterTools(final Screen screen, Delegate delegate) {
    this.screen = screen;
    this.delegate = delegate;
    logger = screen.getLogger();
    sampleService = screen.manager().sampleService();

    filterTools = new HorizontalPanel();
    initWidget(filterTools);

    filterTools.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    filterTools.addStyleName("filterTools");

    Button b = Utils.makeButton("Data...", () -> showDatasetSelector());
    b.addStyleName("lightButton");
    filterTools.add(b);

    dfe = new DataFilterEditor(screen) {
      @Override
      protected void setSampleClass(SampleClass sc) {
        super.setSampleClass(sc);
        FilterTools.this.delegate.filterToolsSampleClassChanged(sc);
      }
    };
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
    DatasetSelector dsel =
        new DatasetSelector(Arrays.asList(info.datasets()),
            Arrays.asList(chosenDatasets)) {
          @Override
          public void onOK() {
            datasetsChanged(getSelected().toArray(new Dataset[0]));
            delegate.filterToolsDatasetsChanged(chosenDatasets);
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

  public void datasetsChanged(Dataset[] ds) {
    chosenDatasets = ds;
    getSampleClasses();
  }

  protected void getSampleClasses() {
    logger.info("Request sample classes for " + chosenDatasets.length + " datasets");
    sampleService.chooseDatasets(chosenDatasets, new PendingAsyncCallback<SampleClass[]>(screen,
        "Unable to choose datasets") {
      @Override
      public void handleSuccess(SampleClass[] sampleClasses) {
        dfe.setAvailable(sampleClasses);
      }
    });
  }
  
  public void sampleClassChanged(SampleClass sc) {
    dfe.sampleClassChanged(sc);
  }
}
