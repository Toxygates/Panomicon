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

package otgviewer.client.components.compoundsel;

import java.util.*;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.NoSelectionModel;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import t.common.client.components.SetEditor;
import t.common.shared.Dataset;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.components.StackedListEditor;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;

/**
 * This widget is for selecting a compound or a set of compounds using various data sources.
 * 
 * Receives: dataFilter Emits: compounds
 */
public class CompoundSelector extends Composite implements RequiresResize, StackedListEditor.Delegate {

  protected final SampleServiceAsync sampleService;

  protected StackedListEditor compoundEditor;
  private DockLayoutPanel dp;

  private Widget north;
  protected final Screen screen;
  protected final Delegate delegate;
  private final String majorParameter;

  private final static int MAX_AUTO_SEL = 20;

  protected Logger logger;

  protected List<Dataset> chosenDatasets = new ArrayList<Dataset>();
  protected SampleClass chosenSampleClass;
  protected List<String> chosenCompounds = new ArrayList<String>();
  public List<ItemList> chosenItemLists = new ArrayList<ItemList>();

  public Delegate delegate() {
    return delegate;
  }

  public interface Delegate {
    void CompoundSelectorItemListsChanged(List<ItemList> itemLists);

    void CompoundSelectorCompoundsChanged(List<String> compounds);

    void CompoundSelectorSampleClassChanged(SampleClass sampleClass);
  }

  public <T extends Screen & Delegate> CompoundSelector(T screen, String heading,
      boolean withListSelector, boolean withFreeEdit) {
    this(screen, screen, heading, withListSelector, withFreeEdit);
  }

  public CompoundSelector(final Screen screen, Delegate delegate, String heading,
      boolean withListSelector, boolean withFreeEdit) {
    this.screen = screen;
    logger = screen.getLogger();
    this.delegate = delegate;
    this.sampleService = screen.manager().sampleService();
    dp = new DockLayoutPanel(Unit.PX);
    this.majorParameter = screen.schema().majorParameter().id();

    initWidget(dp);
    Label lblCompounds = new Label(heading);
    lblCompounds.addStyleName("heading");
    dp.addNorth(lblCompounds, 40);
    north = lblCompounds;

    String instanceName = screen.manager().appInfo().instanceName();
    boolean isAdjuvant = 
        instanceName.equals("adjuvant") || instanceName.equals("dev");

    final Collection<StringList> predefLists =
        (isAdjuvant ? TemporaryCompoundLists.predefinedLists() : new ArrayList<StringList>());
    compoundEditor =
        new StackedListEditor(this, "compounds", heading, MAX_AUTO_SEL, predefLists,
            withListSelector, withFreeEdit) {
          @Override
          protected void selectionChanged(Set<String> selected) {
            List<String> r = new ArrayList<String>();
            r.addAll(selected);
            Collections.sort(r);
            setCompounds(r);
          }

          @Override
          protected void listsChanged(List<ItemList> itemLists) {
            delegate.CompoundSelectorItemListsChanged(itemLists);
          }
          
          @Override
          public void setSelection(Collection<String> items, @Nullable SetEditor<String> fromSelector) {
            super.setSelection(items, fromSelector);
            if (fromSelector instanceof FreeEdit) {
              Analytics.trackEvent(Analytics.CATEGORY_GENERAL, 
                  Analytics.ACTION_FREE_EDIT_COMPOUNDS);  
            }
          }          
        };

    compoundEditor.displayPicker();
    dp.add(compoundEditor);
    compoundEditor.table().setSelectionModel(new NoSelectionModel<String>());
  }

  @Override
  public void onResize() {
    // Since this is not a ResizeComposite, we need to pass on this signal manually
    dp.onResize();
  }

  public void resizeInterface() {
    dp.setWidgetSize(north, 40);
  }

  public List<String> getCompounds() {
    List<String> r = new ArrayList<String>();
    r.addAll(compoundEditor.getSelection());
    Collections.sort(r);
    return r;
  }

  public void fetchCompounds() {
    sampleService.parameterValues(chosenSampleClass, majorParameter,
        new PendingAsyncCallback<String[]>(screen, "Unable to retrieve values for parameter: "
            + majorParameter) {

          @Override
          public void handleSuccess(String[] result) {
            Arrays.sort(result);
            List<String> r = new ArrayList<String>((Arrays.asList(result)));
            compoundEditor.setItems(r, false, true);
            availableCompoundsChanged(Arrays.asList(result));
            if (!compoundEditor.getSelection().isEmpty()) {
              compoundEditor.triggerChange();
            }
          }
        });
  }

  public void setSelection(List<String> compounds) {
    compoundEditor.setSelection(compounds);
    Collections.sort(compounds);
    setCompounds(compounds);
  }

  private void setCompounds(List<String> compounds) {
    chosenCompounds = compounds;
    delegate.CompoundSelectorCompoundsChanged(compounds);
  }

  public void sampleClassChanged(SampleClass sc) {
      //Window.alert("sc = " + sc + "; chosen = " + chosenSampleClass);
      chosenSampleClass = sc;
      //Window.alert("loading majors compoundSeletor");
      delegate.CompoundSelectorSampleClassChanged(sc);
  }

  @Override
  public void itemListsChanged(List<ItemList> lists) {
    chosenItemLists = lists;
    compoundEditor.setLists(lists);
  }

  public void setChosenCompounds(List<String> compounds) {
    chosenCompounds = compounds;
    setSelection(compounds);
  }

  public void datasetsChanged(List<Dataset> datasets) {
    chosenDatasets = datasets;
  }

  protected void availableCompoundsChanged(List<String> compounds) {
  }
}
