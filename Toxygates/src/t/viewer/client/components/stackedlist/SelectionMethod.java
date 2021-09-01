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
package t.viewer.client.components.stackedlist;

import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import t.gwt.common.client.components.SetEditor;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A selection method is a particular user interface for editing the list.
 * It calls back to the StackedListEditor when the selection changes.
 */
public abstract class SelectionMethod<T> extends ResizeComposite implements SetEditor<T> {
    protected final @Nullable SetEditor<T> parentSelector;        
    protected Set<T> currentSelection = new HashSet<T>();
    
    /**
     * @param stackedEditor The editor that this selection method belongs to.
     */
    public SelectionMethod(SetEditor<T> parentSelector) {
        this.parentSelector = parentSelector;
    } 
    
    /**
     * Get the human-readable title of this selection method.
     */
    @Override
    public abstract String getTitle();
    
    /**
     * Set the available items.
     * @param items available items
     * @param clearSelection whether the selection is to be cleared
     * @param alreadySorted whether the items are sorted in order or not.
     */
    public void setItems(List<T> items, boolean clearSelection, boolean alreadySorted) { }
    
    /**
     * Set the currently selected items, reflecting the selection in the GUI.
     * This should not cause changeSelection() to be called.
     * The items should already have been validated.
     */    
    @Override
    public void setSelection(Collection<T> items, @Nullable SetEditor<T> fromEditor) {
      setSelection(items);
    }
    
    @Override
    public Set<T> validateItems(List<T> items) {
      if (parentSelector != null) {
        return parentSelector.validateItems(items);
      }
      return new HashSet<T>(items);
    }
    
    @Override
    public List<T> availableItems() {
      if (parentSelector != null) {
        return parentSelector.availableItems();
      }
      return new LinkedList<T>();
    }
    
    @Override
    public List<Suggestion> getSuggestions(String request) {
      if (parentSelector != null) {
        return parentSelector.getSuggestions(request);
      }
      return new LinkedList<Suggestion>();
    }
    
    @Override
    public Set<T> getSelection() {
      return currentSelection;
    }
    
    @Override
    public void setSelection(Collection<T> items) {
      currentSelection.clear();
      currentSelection.addAll(items);
    }
}
