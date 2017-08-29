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

package t.common.client.components;

import java.util.*;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

/**
 * A SetEditor tracks a selection and makes available a method for modifying it.
 * 
 */
public interface SetEditor<T> {

  /**
   * Optionally set the available items. This clears the selection. May not be useful for all
   * selection methods. The available items are already validated. Some edit methods may display the
   * items in the order given, but they should all be unique.
   * 
   * @param items
   */
  public void setItems(List<T> items, boolean clearSelection);

  public void setSelection(Collection<T> items);

  public void setSelection(Collection<T> items, @Nullable SetEditor<T> fromSelector);

  public Set<T> getSelection();

  public List<T> availableItems();

  public Set<T> validateItems(List<T> items);

  public List<Suggestion> getSuggestions(String request);

}
