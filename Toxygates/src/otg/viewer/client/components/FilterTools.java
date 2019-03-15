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

package otg.viewer.client.components;

import java.util.*;
import java.util.logging.Logger;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import otg.viewer.client.screen.data.DataFilterEditor;
import t.common.client.Utils;
import t.common.shared.Dataset;
import t.model.SampleClass;
import t.viewer.client.components.DatasetSelector;
import t.viewer.client.components.PendingAsyncCallback;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.shared.AppInfo;

/**
 * Tools to select from the available datasets, and then from
 * the available sample macro classes within those datasets.
 */
public class FilterTools extends Composite implements DataFilterEditor.Delegate {
  private HorizontalPanel filterTools;
  private DataFilterEditor dfe;
  final OTGScreen screen;
  final Delegate delegate;
  final SampleServiceAsync sampleService;

  private Logger logger;

  protected List<Dataset> chosenDatasets = new ArrayList<Dataset>();

  public interface Delegate {
    void filterToolsSampleClassChanged(SampleClass sc);
    void filterToolsDatasetsChanged(List<Dataset> ds);
  }

  public <T extends OTGScreen & Delegate> FilterTools(T screen) {
    this(screen, screen);
  }

  public FilterTools(final OTGScreen screen, Delegate delegate) {
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

    dfe = new DataFilterEditor(screen, this);
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
        new DatasetSelector(info.datasets(),
            chosenDatasets) {
          @Override
          public void onOK() {
            datasetsChanged(new ArrayList<Dataset>(getSelected()));
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

  public void datasetsChanged(List<Dataset> datasets) {
    chosenDatasets = datasets;
    getSampleClasses();
  }

  protected void getSampleClasses() {
    logger.info("Request sample classes for " + chosenDatasets.size() + " datasets");
    sampleService.chooseDatasets(chosenDatasets.toArray(new Dataset[0]), new PendingAsyncCallback<SampleClass[]>(screen,
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
  
  
  @Override
  public void dataFilterEditorSampleClassChanged(SampleClass sc) {
    delegate.filterToolsSampleClassChanged(sc);
  }
}
