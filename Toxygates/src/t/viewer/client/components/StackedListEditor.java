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

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import t.common.client.components.SetEditor;
import t.common.client.components.StringSelectionTable;
import t.viewer.client.Utils;
import t.viewer.client.storage.NamedObjectStorage;
import t.viewer.shared.StringList;

/**
 * A StackedListEditor unifies multiple different methods of editing a list of strings. Strings can
 * be: compounds, genes, probes, ...
 *
 */
public class StackedListEditor extends ResizeComposite implements SetEditor<String> {

  protected List<SelectionMethod<String>> methods = new ArrayList<SelectionMethod<String>>();
  protected Set<String> selectedItems = new HashSet<String>();
  protected Set<String> availableItems = new HashSet<String>();
  protected Map<String, String> caseCorrectItems = new HashMap<String, String>();
  protected Collection<StringList> predefinedLists;

  protected StringSelectionTable selTable = null;
  protected DockLayoutPanel dockLayoutPanel;
  protected StackLayoutPanel stackLayoutPanel;
  private BrowseCheck browseCheck;

  protected VerticalPanel northVp;

  protected @Nullable ListChooser listChooser;

  /**
   * @param itemTitle Header for the item type being selected (in certain cases)
   * @param predefinedLists Predefined lists that the user may choose from
   */
  public StackedListEditor(String listType, String itemTitle,
      int maxAutoSel, Collection<StringList> predefinedLists,
      NamedObjectStorage<StringList> stringListStorage,
      boolean withListSelector, boolean withFreeEdit) {
    dockLayoutPanel = new DockLayoutPanel(Unit.PX);
    initWidget(dockLayoutPanel);

    this.predefinedLists = predefinedLists;

    northVp = Utils.mkVerticalPanel();
    northVp.setWidth("100%");

    if (withListSelector) {
      listChooser = new ListChooser(predefinedLists, listType) {
        @Override
        protected void itemsChanged(List<String> items) {
          setSelection(validateItems(items), StackedListEditor.this);
        }
      };
      listChooser.storage = stringListStorage;
      listChooser.addStyleName("colored");
      northVp.add(listChooser);
      dockLayoutPanel.addNorth(northVp, 37);
    }

    createSelectionMethods(methods, itemTitle, maxAutoSel, withFreeEdit);
    if (methods.size() == 1) {
      dockLayoutPanel.add(methods.get(0));
    } else {
      stackLayoutPanel = new StackLayoutPanel(Unit.PX);
      dockLayoutPanel.add(stackLayoutPanel);
      for (SelectionMethod<String> m : methods) {
        stackLayoutPanel.add(m, m.getTitle(), 30);
      }
      stackLayoutPanel.showWidget(browseCheck);
    }
  }

  /**
   * Instantiates the selection methods that are to be used.
   * 
   * @param methods list to add methods to.
   * @return
   */
  protected void createSelectionMethods(List<SelectionMethod<String>> methods, 
      String itemTitle, int maxAutoSel, boolean withFreeEdit) {
    if (withFreeEdit) {
      methods.add(new FreeEdit(this));
    }
    browseCheck = new BrowseCheck(this, itemTitle, maxAutoSel);
    methods.add(browseCheck);
    this.selTable = browseCheck.selTable;
  }

  /**
   * Obtain the inner string selection table, if it exists. May be null.
   */
  @Nullable
  public StringSelectionTable selTable() {
    return selTable;
  }

  /**
   * See above
   */
  @Nullable
  public CellTable<String> table() {
    if (selTable != null) {
      return selTable.table();
    }
    return null;
  }

  /**
   * Validate items, some of which may have been entered manually. This method may be overridden for
   * efficiency.
   * 
   * @param items
   * @return Valid items.
   */
  @Override
  public Set<String> validateItems(List<String> items) {
    HashSet<String> r = new HashSet<String>();

    Iterator<String> i = items.iterator();
    String s = i.next();
    while (s != null) {
      String v = validateItem(s);
      if (v != null) {
        r.add(v);
        if (i.hasNext()) {
          s = i.next();
        } else {
          s = null;
        }
      } else {
        if (i.hasNext()) {
          String s2 = i.next();
          v = validateWithInfixes(s, s2);
          if (v != null) {
            r.add(v);
            if (i.hasNext()) {
              s = i.next();
            } else {
              s = null;
            }
          } else {
            // Give up and treat s2 normally
            s = s2;
          }
        } else {
          s = null;
        }
      }
    }
    return r;
  }

  private String validateWithInfixes(String s1, String s2) {
    // Some compounds have commas in their names but we also split compounds
    // on commas.
    // E.g. 2,4-dinitrophenol and 'imatinib, methanesulfonate salt'
    // Test two together to get around this.
    final String[] infixes = new String[] {",", ", "};
    for (String i : infixes) {
      String test = s1 + i + s2;
      String v = validateItem(test);
      if (v != null) {
        return v;
      }
    }
    return null;
  }

  /**
   * Validate a single item.
   * 
   * @param item
   * @return The valid form (with case corrections etc) of the item.
   */
  protected @Nullable String validateItem(String item) {
    String lower = item.toLowerCase();
    if (caseCorrectItems.containsKey(lower)) {
      return caseCorrectItems.get(lower);
    } else {
      return null;
    }
  }

  @Override
  public Set<String> getSelection() {
    return selectedItems;
  }

  /**
   * Set the available items.
   */
  public void setItems(List<String> items, boolean clearSelection, boolean alreadySorted) {
    caseCorrectItems.clear();
    for (String i : items) {
      caseCorrectItems.put(i.toLowerCase(), i);
    }
    for (SelectionMethod<String> m : methods) {
      m.setItems(items, clearSelection, alreadySorted);
    }
    availableItems = new HashSet<String>(items);
    setSelection(selectedItems.stream().filter(item -> availableItems.contains(item)).collect(Collectors.toList()),
        null);
  }

  public void setLists(List<StringList> lists) {
    if (listChooser != null) {
      listChooser.setLists(lists);
    }
  }

  /**
   * Change the selection.
   * 
   * @param items New selection
   * @param from The selection method that triggered the change, or null if the change was triggered
   *        externally. These items should already be validated.
   */
  @Override
  public void setSelection(Collection<String> items, @Nullable SetEditor<String> from) {
    //logger.info("Receive selection " + items.size() + " from "
    // + (from != null ? from.getClass().toString() : "null"));
    for (SelectionMethod<String> m : methods) {
      if (m != from) {
        m.setSelection(items);
      }
    }
    selectedItems = new HashSet<String>(items);
    if (listChooser != null) {
      listChooser.setItems(new ArrayList<String>(items));
      if (from == null || items.size() == 0) {
        listChooser.resetSelection();
      }
    }
    if (from != null) {
      triggerChange();
    }
  }

  @Override
  public void setSelection(Collection<String> items) {
    setSelection(items, null);
  }

  public void triggerChange() {
    selectionChanged(selectedItems);
  }
  
  /**
   * Outgoing signal. Called when the selection has changed.
   */
  protected void selectionChanged(Set<String> items) {}

  public void scrollBrowseCheckToTop() {
    browseCheck.scrollToTop();
  }

  @Override
  public List<Suggestion> getSuggestions(String request) {
    String lc = request.toLowerCase();
    List<Suggestion> r = new ArrayList<Suggestion>();
    for (String k : caseCorrectItems.keySet()) {
      if (k.startsWith(lc)) {
        final String suggest = caseCorrectItems.get(k);
        r.add(new Suggestion() {
          @Override
          public String getReplacementString() {
            return suggest;
          }

          @Override
          public String getDisplayString() {
            return suggest;
          }
        });
      }
    }
    return r;
  }
  
  @Override
  public List<String> availableItems() {
    return new LinkedList<String>(availableItems);
  }
}
