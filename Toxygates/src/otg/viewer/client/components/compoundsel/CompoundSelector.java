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

package otg.viewer.client.components.compoundsel;

import java.util.*;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.NoSelectionModel;

import otg.viewer.client.components.OTGScreen;
import t.common.client.components.SetEditor;
import t.viewer.client.Analytics;
import t.viewer.client.components.FreeEdit;
import t.viewer.client.components.StackedListEditor;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.client.storage.NamedObjectStorage;
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
  protected final OTGScreen screen;
  protected final Delegate delegate;

  private final static int MAX_AUTO_SEL = 20;

  protected Logger logger;

  List<String> chosenCompounds = new ArrayList<String>();
  public List<StringList> compoundLists = new ArrayList<StringList>();
  
  private List<String> allCompounds;

  public Delegate delegate() {
    return delegate;
  }

  public interface Delegate {
    void compoundSelectorCompoundListsChanged(List<StringList> stringLists);
    void compoundSelectorCompoundsChanged(List<String> compounds);
  }

  public <T extends OTGScreen & Delegate> CompoundSelector(T screen, String heading,
      boolean withListSelector, boolean withFreeEdit) {
    this(screen, screen, heading, withListSelector, withFreeEdit);
  }

  public CompoundSelector(final OTGScreen screen, Delegate delegate, String heading,
      boolean withListSelector, boolean withFreeEdit) {
    this.screen = screen;
    logger = screen.getLogger();
    this.delegate = delegate;
    this.sampleService = screen.manager().sampleService();
    dp = new DockLayoutPanel(Unit.PX);

    initWidget(dp);
    Label lblCompounds = new Label(heading);
    lblCompounds.addStyleName("heading");
    dp.addNorth(lblCompounds, 40);
    north = lblCompounds;

    String instanceName = screen.manager().appInfo().instanceName();
    boolean isAdjuvant = 
        instanceName.equals("adjuvant") || instanceName.equals("dev");

    NamedObjectStorage<StringList> compoundListsStorage = 
        new NamedObjectStorage<StringList>(screen.getStorage().compoundListsStorage, 
            list -> list.name());
    
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
            if (!chosenCompounds.equals(r)) {
              chosenCompounds = r;
              delegate.compoundSelectorCompoundsChanged(r);
            }
          }

          @Override
          protected void listsChanged(List<StringList> stringLists) {
            delegate.compoundSelectorCompoundListsChanged(stringLists);
          }
          
          @Override
          public void setSelection(Collection<String> items, @Nullable SetEditor<String> fromSelector) {
            super.setSelection(items, fromSelector);
            if (fromSelector instanceof FreeEdit) {
              Analytics.trackEvent(Analytics.CATEGORY_GENERAL, 
                  Analytics.ACTION_FREE_EDIT_COMPOUNDS);  
            }
          }     
          
          @Override
          protected boolean checkName(String name) {
            return compoundListsStorage.validateNewObjectName(name, false);
          }
        };

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
  
  public List<String> allCompounds() {
    return allCompounds;
  }
  
  public void acceptCompounds(String[] compounds) {
    Arrays.sort(compounds);
    allCompounds = new ArrayList<String>((Arrays.asList(compounds)));
    compoundEditor.setItems(allCompounds, false, true);
    availableCompoundsChanged(Arrays.asList(compounds));
  }

  public void setSelection(List<String> compounds) {
    compoundEditor.setSelection(compounds);
  }

  public void setChosenCompounds(List<String> compounds) {
    chosenCompounds = compounds;
    setSelection(compounds);
  }

  protected void availableCompoundsChanged(List<String> compounds) {
  }
  
  // StackedListEditor.Delegate methods
  @Override
  public void compoundListsChanged(List<StringList> lists) {
    compoundLists = lists;
    compoundEditor.setLists(lists);
  }  
}
