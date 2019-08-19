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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import t.common.client.components.StringSelectionTable;
import t.viewer.client.Utils;

/**
 * A selection method for StackedListEditor that allows the user to browse a list and check 
 * items with checkboxes. This is only recommended if the total number of available items 
 * is small (< 1000).
 */
public class BrowseCheck extends SelectionMethod<String> {
  protected StringSelectionTable selTable;
  private DockLayoutPanel dlp = new DockLayoutPanel(Unit.PX);
  private Button sortButton;
  private ScrollPanel scrollPanel;

  public BrowseCheck(StackedListEditor editor, String itemTitle, final int maxAutoSel) {
    super(editor);
    initWidget(dlp);

    final BrowseCheck bc = this;
    this.selTable = new StringSelectionTable("", itemTitle) {
      @Override
      protected void selectionChanged(Set<String> selected) {
        //BrowseCheck.logger.info("Send selection " + selected.size());
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
}
