package t.viewer.client.components;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import t.common.client.components.SetEditor;

import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;


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
     * @param items
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
    
    public List<Suggestion> getSuggestions(String request) {
      if (parentSelector != null) {
        return parentSelector.getSuggestions(request);
      }
      return new LinkedList<Suggestion>();
    }
    
    public Set<T> getSelection() {
      return currentSelection;
    }
    
    public void setSelection(Collection<T> items) {
      currentSelection.clear();
      currentSelection.addAll(items);
    }
}