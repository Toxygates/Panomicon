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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import otg.viewer.client.screen.data.DataFilterEditor;
import t.common.client.Utils;
import t.common.shared.Dataset;
import t.model.SampleClass;
import t.viewer.client.components.DatasetSelector;
import t.viewer.client.future.Future;
import t.viewer.shared.AppInfo;

/**
 * Tools to select from the available datasets, and then from
 * the available sample macro classes within those datasets.
 */
public class FilterTools extends Composite implements DataFilterEditor.Delegate {
  private HorizontalPanel filterTools;
  public DataFilterEditor dataFilterEditor;
  final OTGScreen screen;
  final Delegate delegate;

  private Logger logger;

  protected List<Dataset> chosenDatasets = new ArrayList<Dataset>();

  public interface Delegate {
    void filterToolsSampleClassChanged(SampleClass sc);
    Future<?> filterToolsDatasetsChanged(List<Dataset> ds, Future<SampleClass[]> future);
    Future<SampleClass[]> fetchSampleClasses(Future<SampleClass[]> future,
        List<Dataset> datasets);
  }

  public <T extends OTGScreen & Delegate> FilterTools(T screen) {
    this(screen, screen);
  }

  public FilterTools(final OTGScreen screen, Delegate delegate) {
    this.screen = screen;
    this.delegate = delegate;
    logger = screen.getLogger();

    filterTools = new HorizontalPanel();
    initWidget(filterTools);

    filterTools.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    filterTools.addStyleName("filterTools");

    Button b = Utils.makeButton("Data...", () -> showDatasetSelector());
    b.addStyleName("lightButton");
    filterTools.add(b);

    dataFilterEditor = new DataFilterEditor(screen, this);
    filterTools.add(dataFilterEditor);
  }

  protected void showDatasetSelector() {    
    //Re-retrieve user data here (in case user key has changed) and re-populate
    //datasets
    
    logger.info("fetching app info");
    screen.manager().reloadAppInfo(new AsyncCallback<AppInfo>() {      
      @Override
      public void onSuccess(AppInfo result) {
        logger.info("app info fetched");
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
            setDatasets(new ArrayList<Dataset>(getSelected()));
            delegate.filterToolsDatasetsChanged(chosenDatasets,
                delegate.fetchSampleClasses(new Future<SampleClass[]>(), chosenDatasets));
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

  public void setDatasets(List<Dataset> datasets) {
    chosenDatasets = datasets;
  }
  
  public void setSampleClass(SampleClass sc) {
    dataFilterEditor.setSampleClass(sc);
  }
  
  
  @Override
  public void dataFilterEditorSampleClassChanged(SampleClass sc) {
    delegate.filterToolsSampleClassChanged(sc);
  }
}
