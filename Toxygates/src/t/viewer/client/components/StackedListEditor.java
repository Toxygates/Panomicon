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

package t.viewer.client.components;

import java.util.*;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import otgviewer.client.components.ListChooser;
import otgviewer.client.components.ResizableTextArea;
import t.common.client.components.SetEditor;
import t.common.client.components.StringSelectionTable;
import t.common.shared.SharedUtils;
import t.viewer.client.Utils;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;

/**
 * A StackedListEditor unifies multiple different methods of editing a list of strings. Strings can
 * be: compounds, genes, probes, ...
 *
 */
public class StackedListEditor extends ResizeComposite implements SetEditor<String> {

  // private static Logger logger = SharedUtils.getLogger("sle");

  /**
   * A selection method that allows the user to edit a list as text, freely. Items are separated by
   * commas or whitespace.
   */
  public static class FreeEdit extends SelectionMethod<String> {
    // TODO: the constants 10, 45 are somewhat ad-hoc -- find a better method in the future
    protected TextArea textArea = new ResizableTextArea(10, 45);
    private String lastText = "";
    private Timer t;
    private DockLayoutPanel dlp;
    private HorizontalPanel np;    

    public FreeEdit(StackedListEditor editor) {
      super(editor);
      dlp = new DockLayoutPanel(Unit.PX);
      initWidget(dlp);

      Label l = new Label("Search:");
      final SuggestBox sb = new SuggestBox(new SuggestOracle() {
        @Override
        public void requestSuggestions(Request request, Callback callback) {
          callback.onSuggestionsReady(request,
              new Response(parentSelector.getSuggestions(request.getQuery())));
        }
      });
      HorizontalPanel hp = Utils.mkHorizontalPanel(true, l, sb);
      np = Utils.mkWidePanel();
      np.add(hp);

      sb.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
        @Override
        public void onSelection(SelectionEvent<Suggestion> event) {
          Suggestion s = event.getSelectedItem();
          String selection = s.getDisplayString();
          String oldText = textArea.getText().trim();
          String newText = (!"".equals(oldText)) ? (oldText + "\n" + selection) : selection;
          textArea.setText(newText);
          refreshItems(true);
          sb.setText("");
        }
      });

      dlp.addNorth(np, 36);
      textArea.setSize("100%", "100%");
      dlp.add(textArea);
      t = new Timer() {
        @Override
        public void run() {
          refreshItems(false);
        }
      };

      textArea.addKeyUpHandler(new KeyUpHandler() {
        @Override
        public void onKeyUp(KeyUpEvent event) {
          lastText = textArea.getText();
          t.schedule(500);
        }
      });
    }

    private void refreshItems(boolean immediate) {
      final FreeEdit fe = this;
      // Without the immediate flag, only do the refresh action if
      // the text has been unchanged for 500 ms.
      if (immediate || lastText.equals(textArea.getText())) {
        String[] items = parseItems();
        Set<String> valid = parentSelector.validateItems(Arrays.asList(items));
        if (!parentSelector.getSelection().equals(valid)) {
          parentSelector.setSelection(valid, fe);
        }
        currentSelection = valid;
      }
    }

    @Override
    public String getTitle() {
      return "Edit/paste";
    }

    private String[] parseItems() {
      String s = textArea.getText();
      String[] split = s.split("\\s*[,\n]+\\s*");
      return split;
    }

    @Override
    public void setSelection(Collection<String> items) {
      super.setSelection(items);
      textArea.setText(SharedUtils.mkString(items, "\n"));
    }

    @Override
    public void setItems(List<String> items, boolean clearSelection) {
      //no-op
    }
  }

  /**
   * A selection method that allows the user to browse a list and check items with checkboxes. This
   * is only recommended if the total number of available items is small (< 1000)
   */
  public static class BrowseCheck extends SelectionMethod<String> {
    private StringSelectionTable selTable;
    private DockLayoutPanel dlp = new DockLayoutPanel(Unit.PX);
    private Button sortButton;
    private ScrollPanel scrollPanel;

    private static Logger logger = SharedUtils.getLogger("sle.bc");

    public BrowseCheck(StackedListEditor editor, String itemTitle, final int maxAutoSel) {
      super(editor);
      initWidget(dlp);

      final BrowseCheck bc = this;
      this.selTable = new StringSelectionTable("", itemTitle) {
        @Override
        protected void selectionChanged(Set<String> selected) {
          BrowseCheck.logger.info("Send selection " + selected.size());
          parentSelector.setSelection(selected, bc);
          currentSelection = selected;
        }
      };

      HorizontalPanel hp = Utils.mkHorizontalPanel(true);
      hp.setWidth("100%");
      dlp.addSouth(hp, 36);

      sortButton = new Button("Sort by name", new ClickHandler() {
        @Override
        public void onClick(ClickEvent ce) {
          List<String> items = new ArrayList<String>(parentSelector.availableItems());
          Collections.sort(items);
          setItems(items, false, true);
          sortButton.setEnabled(false);
        }
      });
      sortButton.addStyleName("lightButton");
      hp.add(sortButton);
      sortButton.setEnabled(false);

      Button selectAllButton = new Button("Select all", new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          List<String> items = selTable.getItems();
          List<String> sel = items;
          if (items.size() > maxAutoSel) {
            Window.alert("Too many items in list. Only the first " + maxAutoSel
                + " will be selected.");
            sel = items.subList(0, maxAutoSel);
          }

          setSelection(sel);
          parentSelector.setSelection(sel, bc);
        }
      });
      selectAllButton.addStyleName("lightButton");
      hp.add(selectAllButton);

      Button unselectAllButton = new Button("Unselect all", new ClickHandler() {
        @Override
        public void onClick(ClickEvent ce) {
          List<String> empty = new ArrayList<String>();
          setSelection(empty);
          parentSelector.setSelection(empty, bc);
        }
      });
      unselectAllButton.addStyleName("lightButton");
      hp.add(unselectAllButton);

      scrollPanel = new ScrollPanel(selTable);
      dlp.add(scrollPanel);
    }

    @Override
    public String getTitle() {
      return "Browse";
    }

    @Override
    public void setSelection(Collection<String> items) {
      // logger.info("Receive selection " + items.size());
      selTable.setSelection(items);
      selTable.table().redraw();
    }

    @Override
    public void setItems(List<String> items, boolean clearSelection, boolean alreadySorted) {
      selTable.setItems(items, clearSelection);
      sortButton.setEnabled(!alreadySorted);
    }

    public void scrollToTop() {
      scrollPanel.scrollToTop();
    }

    @Override
    public void setItems(List<String> items, boolean clearSelection) {
      // TODO Auto-generated method stub
      
    }
  }

  public interface Delegate {
    void itemListsChanged(List<ItemList> itemLists);
  }

  protected List<SelectionMethod<String>> methods = new ArrayList<SelectionMethod<String>>();
  protected Set<String> selectedItems = new HashSet<String>();
  protected Set<String> availableItems = new HashSet<String>();
  protected Map<String, String> caseCorrectItems = new HashMap<String, String>();
  protected Collection<StringList> predefinedLists;

  protected StringSelectionTable selTable = null;
  protected DockLayoutPanel dlp;
  protected StackLayoutPanel slp;

  protected VerticalPanel northVp;

  protected @Nullable ListChooser listChooser;

  /**
   * @param itemTitle Header for the item type being selected (in certain cases)
   * @param predefinedLists Predefined lists that the user may choose from
   */
  public StackedListEditor(final Delegate delegate,
      String listType, String itemTitle,
      int maxAutoSel, Collection<StringList> predefinedLists,
      boolean withListSelector, boolean withFreeEdit) {
    dlp = new DockLayoutPanel(Unit.PX);
    initWidget(dlp);

    this.predefinedLists = predefinedLists;

    northVp = Utils.mkVerticalPanel();
    northVp.setWidth("100%");

    final StackedListEditor sle = this;

    if (withListSelector) {
      listChooser = new ListChooser(predefinedLists, listType) {
        @Override
        protected void itemsChanged(List<String> items) {
          setSelection(validateItems(items));
        }

        @Override
        protected void listsChanged(List<ItemList> itemLists) {
          delegate.itemListsChanged(itemLists);
          sle.listsChanged(itemLists);
        }
      };
      listChooser.addStyleName("colored");
      northVp.add(listChooser);
      dlp.addNorth(northVp, 37);
    }

    createSelectionMethods(methods, itemTitle, maxAutoSel, withFreeEdit);
    if (methods.size() == 1) {
      dlp.add(methods.get(0));
    } else {
      slp = new StackLayoutPanel(Unit.PX);
      dlp.add(slp);
      for (SelectionMethod<String> m : methods) {
        slp.add(m, m.getTitle(), 30);
      }
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
    BrowseCheck bc = new BrowseCheck(this, itemTitle, maxAutoSel);
    methods.add(bc);
    this.selTable = bc.selTable;
  }

  /**
   * Obtain the inner string selection table, if it exists. May be null. TODO: improve architecture
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

  @Override
  public void setItems(List<String> items, boolean clearSelection) {
    setItems(items, clearSelection, false);
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
  }

  public void setLists(List<ItemList> lists) {
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
    }
    triggerChange();
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

  // Ditto
  protected void listsChanged(List<ItemList> itemLists) {}

  public void clearSelection() {
    setSelection(new HashSet<String>());
  }

  /**
   * Display the picker method, if one exists.
   */
  public void displayPicker() {
    if (methods.size() == 1 || slp == null) {
      return;
    }
    for (SelectionMethod<String> m : methods) {
      if (m instanceof BrowseCheck) {
        slp.showWidget(m);
        ((BrowseCheck) m).scrollToTop();
        return;
      }
    }
    // Should not get here!
    Window.alert("Technical error: no such selection method in StackedListEditor");
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
