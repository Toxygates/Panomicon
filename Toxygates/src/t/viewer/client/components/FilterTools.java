/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

package t.viewer.client.components;

import com.google.gwt.user.client.ui.*;
import t.common.client.Utils;
import t.shared.common.Dataset;
import t.model.SampleClass;
import t.viewer.client.future.Future;
import t.viewer.client.screen.Screen;
import t.viewer.client.screen.data.DataFilterEditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Tools to select from the available datasets, and then from
 * the available sample macro classes within those datasets.
 */
public class FilterTools extends Composite implements DataFilterEditor.Delegate {
  private HorizontalPanel filterTools;
  public DataFilterEditor dataFilterEditor;
  final Screen screen;
  final Delegate delegate;

  private Logger logger;

  protected List<Dataset> chosenDatasets = new ArrayList<Dataset>();

  public interface Delegate {
    void filterToolsSampleClassChanged(SampleClass sc);
    Future<?> filterToolsDatasetsChanged(List<Dataset> ds);
    Future<SampleClass[]> fetchSampleClasses(Future<SampleClass[]> future,
        List<Dataset> datasets);
  }

  public <T extends Screen & Delegate> FilterTools(T screen) {
    this(screen, screen);
  }

  public FilterTools(final Screen screen, Delegate delegate) {
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
    Future<Dataset[]> future = screen.manager().updateDatasets();
    future.addSuccessCallback(datasets -> {
      proceedShowSelector(datasets);
    });
  }
  
  protected void proceedShowSelector(Dataset[] datasets) {
    final DialogBox db = new DialogBox(false, true);    
    DatasetSelector dsel =
        new DatasetSelector(Arrays.asList(datasets),
            chosenDatasets) {
          @Override
          public void onOK() {
            setDatasets(new ArrayList<Dataset>(getSelected()));
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
