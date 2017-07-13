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

package otgviewer.client.components.compoundsel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import t.common.shared.ItemList;
import t.model.SampleClass;
import t.common.shared.StringList;
import t.viewer.client.components.StackedListEditor;
import t.viewer.client.rpc.SampleServiceAsync;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.NoSelectionModel;

/**
 * This widget is for selecting a compound or a set of compounds using various data sources.
 * 
 * Receives: dataFilter Emits: compounds
 */
public class CompoundSelector extends DataListenerWidget implements RequiresResize {

  protected final SampleServiceAsync sampleService;

  protected StackedListEditor compoundEditor;
  private DockLayoutPanel dp;

  private Widget north;
  protected final Screen screen;
  private final String majorParameter;

  private final static int MAX_AUTO_SEL = 20;

  public CompoundSelector(final Screen screen, String heading, boolean withListSelector,
      boolean withFreeEdit) {
    this.screen = screen;
    this.sampleService = screen.manager().sampleService();
    dp = new DockLayoutPanel(Unit.PX);
    this.majorParameter = screen.schema().majorParameter();

    initWidget(dp);
    Label lblCompounds = new Label(heading);
    lblCompounds.setStylePrimaryName("heading");
    dp.addNorth(lblCompounds, 40);
    north = lblCompounds;

    // TODO factor out
    boolean isAdjuvant = screen.manager().getUIType().equals("adjuvant");

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
            changeCompounds(r);
          }

          @Override
          protected void listsChanged(List<ItemList> itemLists) {
            screen.itemListsChanged(itemLists);
            screen.storeItemLists(getParser(screen));
          }
        };

    compoundEditor.displayPicker();
    dp.add(compoundEditor);
    compoundEditor.table().setSelectionModel(new NoSelectionModel<String>());
  }

  @Override
  public void sampleClassChanged(SampleClass sc) {
    super.sampleClassChanged(sc);
    screen.enqueue(new Screen.QueuedAction("loadCompounds") {
      @Override
      public void run() {
        loadMajors();
        // compoundEditor.clearSelection();
      }
    });
  }

  @Override
  public void itemListsChanged(List<ItemList> lists) {
    super.itemListsChanged(lists);
    compoundEditor.setLists(lists);
  }

  public List<String> getCompounds() {
    List<String> r = new ArrayList<String>();
    r.addAll(compoundEditor.getSelection());
    Collections.sort(r);
    return r;
  }

  void loadMajors() {
    sampleService.parameterValues(chosenSampleClass, majorParameter,
        new PendingAsyncCallback<String[]>(this, "Unable to retrieve values for parameter: "
            + majorParameter) {

          @Override
          public void handleSuccess(String[] result) {
            Arrays.sort(result);
            List<String> r = new ArrayList<String>((Arrays.asList(result)));
            compoundEditor.setItems(r, false, true);
            changeAvailableCompounds(Arrays.asList(result));
            if (!compoundEditor.getSelection().isEmpty()) {
              compoundEditor.triggerChange();
            }
          }
        });
  }

  @Override
  public void compoundsChanged(List<String> compounds) {
    super.compoundsChanged(compounds);
    setSelection(compounds);
  }

  public void setSelection(List<String> compounds) {
    compoundEditor.setSelection(compounds);
    Collections.sort(compounds);
    changeCompounds(compounds);
  }

  @Override
  public void onResize() {
    // Since this is not a ResizeComposite, we need to pass on this signal manually
    dp.onResize();
  }

  public void resizeInterface() {
    dp.setWidgetSize(north, 40);
  }

}
